package com.jiil.secretnoti.data

import androidx.room.withTransaction
import com.jiil.secretnoti.capture.NotificationParser
import com.jiil.secretnoti.capture.ParsedNotification
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ConversationUi
import com.jiil.secretnoti.ui.model.MessageUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ChatRepository(private val db: AppDatabase) {
    private val dao = db.chatDao()

    /** 알림 하나를 저장한다. 같은 방의 메시지는 한 대화방으로 모으고, 이미 있는 메시지는 건너뛴다. 대화방 id를 돌려준다. */
    suspend fun ingest(source: Source, packageName: String, parsed: ParsedNotification, receivedAt: Long): Long =
        db.withTransaction {
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
                    if (dao.insertMessage(entity) == -1L) continue
                    if (!m.isMine) newUnread++
                    if (latest == null || entity.postedAt >= latest.postedAt) latest = entity
                } else if (old.text != m.text && !old.isDeletedBySender && NotificationParser.isDeletionMarker(m.text)) {
                    // 같은 메시지가 "삭제된 메시지입니다"로 바뀌었다 → 원문을 남기고 삭제 표시.
                    dao.updateMessage(old.copy(isDeletedBySender = true, originalText = old.text))
                }
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
            conversationId
        }

    /** 설정에서 켠 대상 앱의 대화방만 내보낸다. */
    fun observeConversations(): Flow<List<ConversationUi>> =
        combine(dao.observeConversations(), Prefs.enabledSources) { list, enabled ->
            list.map { it.toUi() }.filter { it.source in enabled }
        }

    fun observeConversation(id: Long): Flow<ConversationUi?> = dao.observeConversation(id).map { it?.toUi() }

    fun observeMessages(conversationId: Long): Flow<List<MessageUi>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toUi() } }

    fun observeIncomingSince(since: Long): Flow<Int> = dao.observeIncomingSince(since)

    suspend fun markSeen(conversationId: Long) = dao.markSeen(conversationId)

    /** 보관 기간이 지난 메시지와 빈 대화방을 지운다. days가 0 이하면 아무것도 지우지 않는다. */
    suspend fun deleteOlderThan(days: Int, now: Long = System.currentTimeMillis()) {
        if (days <= 0) return
        db.withTransaction {
            dao.deleteMessagesBefore(now - days * 24L * 60 * 60 * 1000)
            dao.deleteEmptyConversations()
        }
    }
}

private fun ConversationEntity.toUi() = ConversationUi(
    id = id,
    source = runCatching { Source.valueOf(source) }.getOrDefault(Source.SMS),
    packageName = packageName,
    title = roomTitle,
    isGroup = isGroup,
    lastSender = lastSender,
    preview = lastText,
    lastAt = lastMessageAt,
    unread = unreadCount,
)

private fun MessageEntity.toUi() = MessageUi(
    id = id,
    sender = sender,
    isMine = isMine,
    text = originalText ?: text,
    at = postedAt,
    deletedBySender = isDeletedBySender,
)
