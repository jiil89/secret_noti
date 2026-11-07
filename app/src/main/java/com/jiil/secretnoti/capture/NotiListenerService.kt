package com.jiil.secretnoti.capture

import android.app.Notification
import android.content.ComponentName
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.data.NotificationDump
import com.jiil.secretnoti.overlay.EdgeOverlayController
import com.jiil.secretnoti.settings.Prefs
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
    private var overlay: EdgeOverlayController? = null
    private var overlayJob: Job? = null

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
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        record(sbn, "removed", reason)
    }

    private fun stopOverlay() {
        overlayJob?.cancel()
        overlayJob = null
        overlay?.hide()
        overlay = null
    }

    private fun record(sbn: StatusBarNotification, event: String, reason: Int?) {
        val pkg = sbn.packageName
        if (pkg == packageName) return
        if (!Prefs.dumpAllApps.value && TargetApps.sourceOf(this, pkg) == null) return

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
