package com.jiil.secretnoti.data

import android.content.Context
import androidx.room.withTransaction
import com.jiil.secretnoti.capture.AppLabels
import com.jiil.secretnoti.capture.KakaoEmoticons
import com.jiil.secretnoti.capture.NotificationParser
import com.jiil.secretnoti.capture.ParsedMessage
import com.jiil.secretnoti.capture.ParsedNotification
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ConversationUi
import com.jiil.secretnoti.ui.model.MessageUi
import com.jiil.secretnoti.ui.model.SearchResultUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File

/** 새로 저장된 메시지와, 이번 알림으로 삭제가 확인된 메시지. */
data class IngestResult(
    val conversationId: Long,
    val roomTitle: String,
    val inserted: List<Pair<Long, ParsedMessage>>,
    val deleted: List<MessageEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.chatDao()

    /**
     * 알림 하나를 저장한다. 같은 방의 메시지는 한 대화방으로 모으고, 이미 있는 메시지는 건너뛴다.
     * [removedKeys]는 같은 알림의 이전 버전에서 중간에 빠진 메시지(삭제 추정)의 키.
     */
    suspend fun ingest(
        source: Source,
        packageName: String,
        parsed: ParsedNotification,
        receivedAt: Long,
        removedKeys: List<String> = emptyList(),
    ): IngestResult? = db.withTransaction {
        val existing = dao.findConversation(packageName, parsed.roomKey)
        // 저장을 끈 대화방은 아무것도 저장하지 않는다.
        if (existing?.isMuted == true) return@withTransaction null
        val adNow = parsed.messages.any { NotificationParser.looksLikeAd(it.text) }
        val newConversation = ConversationEntity(
            source = source.name,
            packageName = packageName,
            roomKey = parsed.roomKey,
            roomTitle = parsed.roomTitle,
            isGroup = parsed.isGroup,
            lastMessageAt = 0,
            lastSender = null,
            lastText = "",
            unreadCount = 0,
            looksLikeAd = adNow,
            isChannel = parsed.isChannel,
        )
        val autoMute = (adNow && Prefs.autoMuteAds.value) || (parsed.isChannel && Prefs.autoMuteChannels.value)
        if (existing == null && autoMute) {
            // 광고로 보이는 새 대화방: 방만 기억하고(설정에서 다시 켤 수 있게) 메시지는 저장하지 않는다.
            dao.insertConversation(newConversation.copy(isMuted = true))
            return@withTransaction null
        }
        val conversationId = existing?.id ?: dao.insertConversation(newConversation)

        val inserted = mutableListOf<Pair<Long, ParsedMessage>>()
        val deleted = mutableListOf<MessageEntity>()
        var newUnread = 0
        var latest: MessageEntity? = null
        for (m in parsed.messages) {
            val old = dao.findMessage(conversationId, m.dedupKey)
            if (old == null) {
                val entity = MessageEntity(
                    conversationId = conversationId,
                    dedupKey = m.dedupKey,
                    sender = m.sender,
                    isMine = m.isMine,
                    text = m.text,
                    postedAt = m.postedAt,
                    receivedAt = receivedAt,
                    isDeletedBySender = NotificationParser.isDeletionMarker(m.text),
                    originalText = null,
                    deletedAt = if (NotificationParser.isDeletionMarker(m.text)) receivedAt else 0,
                )
                val id = dao.insertMessage(entity)
                if (id == -1L) continue
                inserted += id to m
                if (!m.isMine) newUnread++
                if (latest == null || entity.postedAt >= latest.postedAt) latest = entity
            } else if (old.text != m.text && !old.isDeletedBySender && NotificationParser.isDeletionMarker(m.text)) {
                // 같은 메시지가 "삭제된 메시지입니다"로 바뀌었다 → 원문을 남기고 삭제 표시.
                val marked = old.copy(isDeletedBySender = true, originalText = old.text, deletedAt = receivedAt)
                dao.updateMessage(marked)
                deleted += marked
            }
        }
        for (key in removedKeys) {
            val old = dao.findMessage(conversationId, key) ?: continue
            if (old.isDeletedBySender || old.isMine) continue
            val marked = old.copy(isDeletedBySender = true, originalText = old.originalText ?: old.text, deletedAt = receivedAt)
            dao.updateMessage(marked)
            deleted += marked
        }

        val conv = dao.getConversation(conversationId)!!
        val newer = latest?.takeIf { it.postedAt >= conv.lastMessageAt }
        // 카톡이 알려 준 안 읽은 수보다 우리가 센 게 많으면, 그 앞의 것은 카톡에서 이미 읽은 것.
        // 가장 오래된 "아직 안 읽은" 메시지 바로 전까지를 읽음으로 본다 (같은 시각에 받은 것은 안 읽은 쪽으로 남김).
        var readAt = conv.readAt
        var unread = conv.unreadCount + newUnread
        parsed.appUnread?.let { appUnread ->
            val oldestUnread = dao.incomingReceivedAt(conversationId, appUnread - 1)
            if (oldestUnread != null && oldestUnread - 1 > readAt) readAt = oldestUnread - 1
            unread = minOf(unread, appUnread)
        }
        dao.updateConversation(
            conv.copy(
                // 단톡방 이름을 모를 때(보낸 사람 이름만 있을 때)는 이미 아는 방 이름을 지킨다.
                roomTitle = if (parsed.titleReliable || conv.lastMessageAt == 0L) parsed.roomTitle else conv.roomTitle,
                isGroup = if (parsed.groupExplicit) parsed.isGroup else conv.isGroup || parsed.isGroup,
                unreadCount = unread,
                readAt = readAt,
                lastMessageAt = newer?.postedAt ?: conv.lastMessageAt,
                lastSender = if (newer != null) newer.sender else conv.lastSender,
                lastText = newer?.text ?: conv.lastText,
                looksLikeAd = conv.looksLikeAd || adNow,
                isChannel = parsed.isChannel,
            ),
        )
        IngestResult(conversationId, parsed.roomTitle, inserted, deleted)
    }

    suspend fun setImagePath(messageId: Long, path: String) = dao.setImagePath(messageId, path)

    suspend fun recordSeenApp(packageName: String, at: Long) = dao.upsertSeenApp(SeenAppEntity(packageName, at))

    fun observeSeenApps(): Flow<List<String>> = dao.observeSeenApps().map { list -> list.map { it.packageName } }

    /** 켠 대상 앱의 대화방 (저장 끈 방 제외). [includeHidden]이 false면 숨긴 대화방은 뺀다. 고정한 방이 먼저 온다. */
    fun observeConversations(includeHidden: Boolean = false): Flow<List<ConversationUi>> =
        observeVisible(includeHidden, panelOnly = false)

    /** 엣지패널용: 패널에서 뺀 뒤로 새 메시지가 온 대화방만. */
    fun observePanelConversations(): Flow<List<ConversationUi>> = observeVisible(includeHidden = false, panelOnly = true)

    fun observeMutedConversations(): Flow<List<ConversationUi>> =
        dao.observeConversations().map { list ->
            list.filter { it.conversation.isMuted }.map {
                it.toUi(previewHidden = Prefs.hidePreviews.value, hasKeyword = false)
                    .copy(isMuted = true, hasRecords = it.conversation.lastText.isNotEmpty())
            }
        }

    private fun observeVisible(includeHidden: Boolean, panelOnly: Boolean): Flow<List<ConversationUi>> =
        combine(
            dao.observeConversations(),
            Prefs.enabledApps.flow,
            Prefs.hidePreviews.flow,
            keywordConversationIds(),
        ) { list, enabled, hidePreviews, keywordIds ->
            val sms = TargetApps.defaultSmsPackage(context)
            list.filter { TargetApps.isEnabled(it.conversation.packageName, enabled, sms) }
                .filter { !it.conversation.isMuted }
                .filter { includeHidden || !it.conversation.isHidden }
                .filter { !panelOnly || it.conversation.lastMessageAt > it.conversation.panelDismissedAt }
                .map { it.toUi(hidePreviews, it.conversation.id in keywordIds) }
        }

    fun observeHiddenConversations(): Flow<List<ConversationUi>> =
        dao.observeConversations().map { list ->
            list.filter { it.conversation.isHidden }.map { it.toUi(previewHidden = false, hasKeyword = false) }
        }

    fun observeConversation(id: Long): Flow<ConversationUi?> =
        dao.observeConversation(id).map { it?.toUi(previewHidden = false, hasKeyword = false) }

    fun observeSenders(conversationId: Long): Flow<List<String>> = dao.observeSenders(conversationId)

    fun observeMessages(conversationId: Long): Flow<List<MessageUi>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toUi() } }

    fun search(query: String): Flow<List<SearchResultUi>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        return dao.search(escapeLike(q)).map { rows ->
            rows.map { r ->
                SearchResultUi(
                    conversationId = r.message.conversationId,
                    messageId = r.message.id,
                    roomTitle = r.roomTitle,
                    packageName = r.packageName,
                    appLabel = AppLabels.label(context, r.packageName),
                    sender = r.message.sender,
                    isMine = r.message.isMine,
                    text = (r.message.originalText ?: r.message.text)
                        .let { if (r.packageName == TargetApps.KAKAO) KakaoEmoticons.toDisplay(it) else it },
                    at = r.message.postedAt,
                    deletedBySender = r.message.isDeletedBySender,
                )
            }
        }
    }

    fun observeIncomingSince(since: Long): Flow<Int> = dao.observeIncomingSince(since)

    suspend fun markSeen(conversationId: Long) = dao.markSeen(conversationId, System.currentTimeMillis())
    suspend fun togglePin(conversationId: Long) = dao.togglePin(conversationId)
    suspend fun setHidden(conversationId: Long, hidden: Boolean) = dao.setHidden(conversationId, hidden)

    /** 이 대화방 저장 끄기. [deleteExisting]이면 지금까지의 기록(사진 포함)도 지운다. */
    suspend fun mute(conversationId: Long, deleteExisting: Boolean) {
        if (deleteExisting) {
            dao.imagePathsOf(conversationId).forEach { File(it).delete() }
            db.withTransaction {
                dao.deleteMessagesOf(conversationId)
                dao.clearSummary(conversationId)
            }
        }
        dao.setMuted(conversationId, true)
    }

    suspend fun unmute(conversationId: Long) = dao.setMuted(conversationId, false)

    /** 아직 저장 중인 카톡 채널·알림톡 대화방들의 저장을 한 번에 끈다. 끈 개수를 돌려준다. */
    suspend fun muteAllChannels(deleteExisting: Boolean): Int {
        val ids = dao.unmutedChannelIds()
        ids.forEach { mute(it, deleteExisting) }
        return ids.size
    }

    /**
     * 채널 구분이 생기기 전에 저장된 카톡 대화방에 채널 표시를 붙인다 (한 번만).
     * 진단용으로 남겨 둔 최근 알림 원본에서 "답장" 버튼이 한 번도 없던 방을 채널로 본다.
     */
    suspend fun backfillChannelsFromDumps() {
        val canReply = HashMap<String, Boolean>()
        for (dump in db.dumpDao().all()) {
            if (dump.packageName != TargetApps.KAKAO || dump.event != "posted") continue
            val json = runCatching { org.json.JSONObject(dump.json) }.getOrNull() ?: continue
            if (json.optBoolean("isGroupSummary")) continue
            val key = json.optString("shortcutId").takeIf { it.isNotEmpty() && it != "null" }
                ?: json.optString("tag").takeIf { it.isNotEmpty() && it != "null" } ?: continue
            val actions = json.optJSONArray("actions")
            val reply = actions != null && (0 until actions.length()).any { actions.getJSONObject(it).optBoolean("hasRemoteInputs") }
            canReply[key] = (canReply[key] ?: false) || reply
        }
        val channels = canReply.filterValues { !it }.keys.toList()
        if (channels.isNotEmpty()) dao.markChannels(TargetApps.KAKAO, channels)
    }

    /**
     * 광고 판별이 바뀌기 전에 저장된 방을 다시 본다 (문자의 "<제목: (광고)…>", "[Web발신] (광고)…").
     * 광고로 보이면 표시를 붙이고, "광고는 처음부터 저장 안 함"이 켜져 있으면 저장도 끈다 (기록은 남김).
     */
    suspend fun backfillAdFlags() {
        for (conv in dao.allConversations()) {
            if (conv.looksLikeAd) continue
            if (dao.messagesOf(conv.id).none { NotificationParser.looksLikeAd(it.text) }) continue
            dao.updateConversation(conv.copy(looksLikeAd = true,
                isMuted = conv.isMuted || Prefs.autoMuteAds.value))
        }
    }

    /** 아직 저장 중인 카톡 채널·알림톡 대화방 (설정의 "한 번에 끄기"용). */
    fun observeChannelConversations(): Flow<List<ConversationUi>> =
        dao.observeConversations().map { list ->
            list.filter { it.conversation.isChannel && !it.conversation.isMuted }
                .map { it.toUi(previewHidden = false, hasKeyword = false) }
        }

    suspend fun dismissFromPanel(conversationId: Long, now: Long = System.currentTimeMillis()) =
        dao.setPanelDismissedAt(conversationId, now)

    suspend fun dismissAllFromPanel(now: Long = System.currentTimeMillis()) = dao.dismissAllFromPanel(now)

    /** 원래 앱에서 먼저 읽었다: 안 읽은 수 0, 설정에 따라 패널에서도 뺀다. */
    suspend fun markReadElsewhere(packageName: String, roomKey: String, removeFromPanel: Boolean) {
        val now = System.currentTimeMillis()
        dao.markReadElsewhere(packageName, roomKey, now, if (removeFromPanel) now else 0)
    }

    /**
     * 앱 아이콘 배지가 0이 됐다: 원래 앱에서 모두 읽었다.
     * 배지가 새 메시지보다 늦게 바뀔 수 있으므로 최근 [graceMs] 안에 온 방은 건드리지 않는다.
     */
    suspend fun markAppAllRead(packageName: String, removeFromPanel: Boolean, graceMs: Long = 15_000) {
        val now = System.currentTimeMillis()
        dao.markAppAllRead(packageName, now - graceMs, now, if (removeFromPanel) now else 0)
    }

    /**
     * 시크릿노티의 "카톡에서 열기"로 그 방을 열었다. 원래 앱에서 읽음 처리되므로 여기서도 읽음으로 본다.
     * 알림창에서 이미 지운 알림이면 원래 앱이 방을 열어도 알림 제거 소식이 안 와서 따로 처리해야 한다.
     */
    suspend fun markOpenedInApp(conversationId: Long, removeFromPanel: Boolean) {
        val now = System.currentTimeMillis()
        dao.markOpenedInApp(conversationId, now, if (removeFromPanel) now else 0)
    }

    suspend fun deleteConversation(conversationId: Long) {
        dao.imagePathsOf(conversationId).forEach { File(it).delete() }
        dao.deleteConversation(conversationId)
    }

    /** 보관 기간이 지난 메시지·사진과 빈 대화방을 지운다. days가 0 이하면 아무것도 지우지 않는다. */
    suspend fun deleteOlderThan(days: Int, now: Long = System.currentTimeMillis()) {
        if (days <= 0) return
        val before = now - days * 24L * 60 * 60 * 1000
        dao.imagePathsBefore(before).forEach { File(it).delete() }
        db.withTransaction {
            dao.deleteMessagesBefore(before)
            dao.deleteEmptyConversations()
        }
    }

    private fun keywordConversationIds(): Flow<Set<Long>> =
        Prefs.keywords.flow.flatMapLatest { keywords ->
            if (keywords.isEmpty()) {
                flowOf(emptySet())
            } else {
                combine(keywords.map { dao.observeConversationsWithKeyword(escapeLike(it)) }) { arrays ->
                    arrays.flatMap { it }.toSet()
                }
            }
        }

    private fun escapeLike(s: String) = s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun ConversationWithStats.toUi(previewHidden: Boolean, hasKeyword: Boolean) = ConversationUi(
        id = conversation.id,
        source = runCatching { Source.valueOf(conversation.source) }.getOrDefault(Source.OTHER),
        packageName = conversation.packageName,
        appLabel = AppLabels.label(context, conversation.packageName),
        title = conversation.roomTitle,
        isGroup = conversation.isGroup,
        lastSender = conversation.lastSender,
        preview = if (conversation.packageName == TargetApps.KAKAO) KakaoEmoticons.toDisplay(conversation.lastText) else conversation.lastText,
        lastAt = conversation.lastMessageAt,
        unread = conversation.unreadCount,
        isPinned = conversation.isPinned,
        isHidden = conversation.isHidden,
        deletedCount = deletedCount,
        hasKeyword = hasKeyword,
        previewHidden = previewHidden,
        looksLikeAd = conversation.looksLikeAd,
        senderCount = senderCount,
        isChannel = conversation.isChannel,
    )
}

private fun MessageEntity.toUi() = MessageUi(
    id = id,
    sender = sender,
    isMine = isMine,
    text = originalText ?: text,
    at = postedAt,
    deletedBySender = isDeletedBySender,
    imagePath = imagePath,
)
