package com.jiil.secretnoti.capture

import android.app.Notification
import android.content.ComponentName
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.data.NotificationDump
import com.jiil.secretnoti.R
import com.jiil.secretnoti.data.IngestResult
import com.jiil.secretnoti.overlay.EdgeOverlayController
import com.jiil.secretnoti.overlay.NewMessageEvent
import com.jiil.secretnoti.overlay.OverlayEvents
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ReadElsewhereAction
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotiListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dao by lazy { NotiApp.instance.db.dumpDao() }
    private val chat by lazy { NotiApp.instance.chat }
    private var overlay: EdgeOverlayController? = null
    private var overlayJob: Job? = null

    /** 알림 key → 그 알림에 마지막으로 담겨 있던 메시지 키들. 중간에서 빠진 메시지(삭제) 찾기용. 메인 스레드 전용. */
    private val lastKeys = HashMap<String, List<String>>()

    /** 같은 앱을 '메시지 알림을 보낸 앱'으로 너무 자주 기록하지 않게. */
    private val seenRecordedAt = HashMap<String, Long>()

    override fun onListenerConnected() {
        ListenerStatus.connected.value = true
        val controller = EdgeOverlayController(this, scope)
        overlay = controller
        overlayJob?.cancel()
        overlayJob = scope.launch {
            combine(Prefs.overlayEnabled.flow, ListenerStatus.refresh) { enabled, _ ->
                enabled && Settings.canDrawOverlays(this@NotiListenerService)
            }.collect { show -> if (show) controller.show() else controller.hide() }
        }
        // 연결이 끊겨 있던 동안 쌓인 알림도 저장하고, 하루 한 번 보관 기간이 지난 기록을 지운다.
        runCatching { activeNotifications }.getOrNull()?.forEach { ingest(it) }
        cleanupIfDue()
        if (!Prefs.channelBackfillDone.value) {
            scope.launch(Dispatchers.IO) {
                runCatching { chat.backfillChannelsFromDumps() }.onSuccess { Prefs.channelBackfillDone.value = true }
            }
        }
        if (!Prefs.adBackfillDone.value) {
            scope.launch(Dispatchers.IO) {
                runCatching { chat.backfillAdFlags() }.onSuccess { Prefs.adBackfillDone.value = true }
            }
        }
    }

    override fun onListenerDisconnected() {
        ListenerStatus.connected.value = false
        stopOverlay()
        // 삼성 절전 등으로 끊기면 다시 붙여 달라고 요청한다.
        requestRebind(ComponentName(this, NotiListenerService::class.java))
    }

    override fun onDestroy() {
        ListenerStatus.connected.value = false
        stopOverlay()
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        record(sbn, "posted", null)
        ingest(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        record(sbn, "removed", reason)
        lastKeys.remove(sbn.key)
        // 원래 앱이 스스로 알림을 지웠거나(그 앱에서 읽음) 알림을 눌러 열었다 → 그 대화방은 읽은 것으로 본다.
        // 사용자가 밀어서 지운 것(REASON_CANCEL, 모두 지우기 REASON_CANCEL_ALL)은 읽은 게 아니므로 그대로 둔다.
        // 그 뒤 카톡에서 읽은 것은 다음 메시지의 안 읽은 수로 알아챈다 (ParsedNotification.appUnread).
        if (reason == REASON_APP_CANCEL || reason == REASON_CLICK) markReadElsewhere(sbn)
    }

    private fun markReadElsewhere(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName || !isTarget(pkg)) return
        val parsed = NotificationParser.parse(SnapshotExtractor.from(sbn)) ?: return
        val removeFromPanel = Prefs.readElsewhere.value == ReadElsewhereAction.REMOVE_FROM_PANEL
        val context = applicationContext
        scope.launch(Dispatchers.IO) {
            runCatching { chat.markReadElsewhere(pkg, parsed.roomKey, removeFromPanel) }
            WidgetUpdater.request(context)
        }
    }

    private fun stopOverlay() {
        overlayJob?.cancel()
        overlayJob = null
        overlay?.hide()
        overlay = null
    }

    private fun isTarget(pkg: String) =
        TargetApps.isEnabled(pkg, Prefs.enabledApps.value, TargetApps.defaultSmsPackage(this))

    private fun ingest(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName) return
        val snapshot = SnapshotExtractor.from(sbn, shortcutLabel(sbn))
        val now = System.currentTimeMillis()

        val messageLike = !snapshot.isOngoing && (snapshot.messages.isNotEmpty() || snapshot.category == Notification.CATEGORY_MESSAGE)
        if (messageLike && now - (seenRecordedAt[pkg] ?: 0) > 60_000) {
            seenRecordedAt[pkg] = now
            scope.launch(Dispatchers.IO) { runCatching { chat.recordSeenApp(pkg, now) } }
        }
        if (!isTarget(pkg)) return

        val source = TargetApps.sourceOf(this, pkg)
        // 카톡·문자 외의 앱은 메시지형 알림만 받는다 (광고·통화·상태 알림 제외).
        val messagesOnly = source == Source.OTHER || source == Source.TEAMS
        val parsed = NotificationParser.parse(snapshot, messagesOnly) ?: return

        val keys = parsed.messages.map { it.dedupKey }
        val removed = if (keys.isNotEmpty() && keys.all { it.startsWith("id:") }) {
            lastKeys[sbn.key]?.let { NotificationParser.removedFromMiddle(it, keys) }.orEmpty()
        } else {
            emptyList()
        }
        lastKeys[sbn.key] = keys

        val contentIntent = sbn.notification.contentIntent
        val context = applicationContext
        scope.launch(Dispatchers.IO) {
            val result = runCatching { chat.ingest(source, pkg, parsed, now, removed) }.getOrNull() ?: return@launch
            contentIntent?.let { AppLauncher.remember(result.conversationId, it) }
            if (Prefs.savePhotos.value) {
                for ((messageId, m) in result.inserted) {
                    val uri = m.imageUri ?: continue
                    PhotoSaver.save(context, uri, result.conversationId, messageId)
                        ?.let { chat.setImagePath(messageId, it) }
                }
            }
            announce(result, pkg, parsed.isGroup)
            if (result.deleted.isNotEmpty() && Prefs.deletionAlerts.value) {
                DeletionNotifier.notify(
                    context, result.conversationId, result.roomTitle, AppLabels.label(context, pkg), result.deleted,
                )
            }
            WidgetUpdater.request(context)
        }
    }

    /** 손잡이 반짝임·미리보기 말풍선용 신호. 내가 보낸 메시지만 있으면 알리지 않는다. */
    private fun announce(result: IngestResult, pkg: String, isGroup: Boolean) {
        val latest = result.inserted.map { it.second }.filter { !it.isMine }.maxByOrNull { it.postedAt } ?: return
        val raw = if (pkg == TargetApps.KAKAO) KakaoEmoticons.toDisplay(latest.text) else latest.text
        val text = when {
            Prefs.hidePreviews.value -> getString(R.string.peek_hidden)
            isGroup && latest.sender != null -> latest.sender + ": " + raw
            else -> raw
        }
        OverlayEvents.newMessage(NewMessageEvent(result.conversationId, pkg, result.roomTitle, text))
    }

    /** 알림의 대화 바로가기 이름 (Android 11+). 카톡 단톡방 이름은 여기에만 있다. */
    private fun shortcutLabel(sbn: StatusBarNotification): String? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        return runCatching {
            val ranking = Ranking()
            if (!currentRanking.getRanking(sbn.key, ranking)) return null
            ranking.conversationShortcutInfo?.let { it.shortLabel?.toString() ?: it.longLabel?.toString() }
        }.getOrNull()
    }

    private fun cleanupIfDue() {
        val now = System.currentTimeMillis()
        if (now - Prefs.lastCleanupAt.value < 24L * 60 * 60 * 1000) return
        Prefs.lastCleanupAt.value = now
        scope.launch(Dispatchers.IO) { runCatching { chat.deleteOlderThan(Prefs.retentionDays.value, now) } }
    }

    private fun record(sbn: StatusBarNotification, event: String, reason: Int?) {
        val pkg = sbn.packageName
        if (pkg == packageName) return
        if (!Prefs.dumpAllApps.value && !isTarget(pkg)) return

        val json = try {
            NotificationDumper.toJson(sbn, event, reason)
        } catch (t: Throwable) {
            JSONObject().put("package", pkg).put("event", event).put("error", t.toString())
        }
        val extras = sbn.notification.extras
        val dump = NotificationDump(
            event = event,
            packageName = pkg,
            postTime = sbn.postTime,
            receivedAt = System.currentTimeMillis(),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            json = json.toString(2),
        )
        scope.launch(Dispatchers.IO) {
            dao.insert(dump)
            dao.trim(keep = 3000)
        }
    }
}
