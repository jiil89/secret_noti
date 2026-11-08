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
        )
        if (existing == null && adNow && Prefs.autoMuteAds.value) {
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
        dao.updateConversation(
            conv.copy(
                // 단톡방 이름을 모를 때(보낸 사람 이름만 있을 때)는 이미 아는 방 이름을 지킨다.
                roomTitle = if (parsed.titleReliable || conv.lastMessageAt == 0L) parsed.roomTitle else conv.roomTitle,
                isGroup = if (parsed.groupExplicit) parsed.isGroup else conv.isGroup || parsed.isGroup,
                unreadCount = conv.unreadCount + newUnread,
                lastMessageAt = newer?.postedAt ?: conv.lastMessageAt,
                lastSender = if (newer != null) newer.sender else conv.lastSender,
                lastText = newer?.text ?: conv.lastText,
                looksLikeAd = conv.looksLikeAd || adNow,
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

    suspend fun dismissFromPanel(conversationId: Long, now: Long = System.currentTimeMillis()) =
        dao.setPanelDismissedAt(conversationId, now)

    suspend fun dismissAllFromPanel(now: Long = System.currentTimeMillis()) = dao.dismissAllFromPanel(now)

    /** 원래 앱에서 먼저 읽었다: 안 읽은 수 0, 설정에 따라 패널에서도 뺀다. */
    suspend fun markReadElsewhere(packageName: String, roomKey: String, removeFromPanel: Boolean) {
        val now = System.currentTimeMillis()
        dao.markReadElsewhere(packageName, roomKey, now, if (removeFromPanel) now else 0)
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
