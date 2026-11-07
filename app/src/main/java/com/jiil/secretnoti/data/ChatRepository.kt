package com.jiil.secretnoti.data

import android.content.Context
import androidx.room.withTransaction
import com.jiil.secretnoti.capture.AppLabels
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
    ): IngestResult = db.withTransaction {
        val conversationId = dao.findConversation(packageName, parsed.roomKey)?.id
            ?: dao.insertConversation(
                ConversationEntity(
                    source = source.name,
                    packageName = packageName,
                    roomKey = parsed.roomKey,
                    roomTitle = parsed.roomTitle,
                    isGroup = parsed.isGroup,
                    lastMessageAt = 0,
                    lastSender = null,
                    lastText = "",
                    unreadCount = 0,
                ),
            )

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
                )
                val id = dao.insertMessage(entity)
                if (id == -1L) continue
                inserted += id to m
                if (!m.isMine) newUnread++
                if (latest == null || entity.postedAt >= latest.postedAt) latest = entity
            } else if (old.text != m.text && !old.isDeletedBySender && NotificationParser.isDeletionMarker(m.text)) {
                // 같은 메시지가 "삭제된 메시지입니다"로 바뀌었다 → 원문을 남기고 삭제 표시.
                val marked = old.copy(isDeletedBySender = true, originalText = old.text)
                dao.updateMessage(marked)
                deleted += marked
            }
        }
        for (key in removedKeys) {
            val old = dao.findMessage(conversationId, key) ?: continue
            if (old.isDeletedBySender || old.isMine) continue
            val marked = old.copy(isDeletedBySender = true, originalText = old.originalText ?: old.text)
            dao.updateMessage(marked)
            deleted += marked
        }

        val conv = dao.getConversation(conversationId)!!
        val newer = latest?.takeIf { it.postedAt >= conv.lastMessageAt }
        dao.updateConversation(
            conv.copy(
                roomTitle = parsed.roomTitle,
                isGroup = conv.isGroup || parsed.isGroup,
                unreadCount = conv.unreadCount + newUnread,
                lastMessageAt = newer?.postedAt ?: conv.lastMessageAt,
                lastSender = if (newer != null) newer.sender else conv.lastSender,
                lastText = newer?.text ?: conv.lastText,
            ),
        )
        IngestResult(conversationId, parsed.roomTitle, inserted, deleted)
    }

    suspend fun setImagePath(messageId: Long, path: String) = dao.setImagePath(messageId, path)

    suspend fun recordSeenApp(packageName: String, at: Long) = dao.upsertSeenApp(SeenAppEntity(packageName, at))

    fun observeSeenApps(): Flow<List<String>> = dao.observeSeenApps().map { list -> list.map { it.packageName } }

    /** 켠 대상 앱의 대화방. [includeHidden]이 false면 숨긴 대화방은 뺀다. 고정한 방이 먼저 온다. */
    fun observeConversations(includeHidden: Boolean = false): Flow<List<ConversationUi>> =
        combine(
            dao.observeConversations(),
            Prefs.enabledApps.flow,
            Prefs.hidePreviews.flow,
            keywordConversationIds(),
        ) { list, enabled, hidePreviews, keywordIds ->
            val sms = TargetApps.defaultSmsPackage(context)
            list.filter { TargetApps.isEnabled(it.conversation.packageName, enabled, sms) }
                .filter { includeHidden || !it.conversation.isHidden }
                .map { it.toUi(hidePreviews, it.conversation.id in keywordIds) }
        }

    fun observeHiddenConversations(): Flow<List<ConversationUi>> =
        dao.observeConversations().map { list ->
            list.filter { it.conversation.isHidden }.map { it.toUi(previewHidden = false, hasKeyword = false) }
        }

    fun observeConversation(id: Long): Flow<ConversationUi?> =
        dao.observeConversation(id).map { it?.toUi(previewHidden = false, hasKeyword = false) }

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
                    text = r.message.originalText ?: r.message.text,
                    at = r.message.postedAt,
                    deletedBySender = r.message.isDeletedBySender,
                )
            }
        }
    }

    fun observeIncomingSince(since: Long): Flow<Int> = dao.observeIncomingSince(since)

    suspend fun markSeen(conversationId: Long) = dao.markSeen(conversationId)
    suspend fun togglePin(conversationId: Long) = dao.togglePin(conversationId)
    suspend fun setHidden(conversationId: Long, hidden: Boolean) = dao.setHidden(conversationId, hidden)

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
        preview = conversation.lastText,
        lastAt = conversation.lastMessageAt,
        unread = conversation.unreadCount,
        isPinned = conversation.isPinned,
        isHidden = conversation.isHidden,
        deletedCount = deletedCount,
        hasKeyword = hasKeyword,
        previewHidden = previewHidden,
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
