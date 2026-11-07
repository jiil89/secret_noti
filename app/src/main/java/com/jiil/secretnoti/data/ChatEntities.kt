package com.jiil.secretnoti.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 대화방. (packageName, roomKey)가 같으면 1:1이든 단톡이든 같은 방으로 모은다. */
@Entity(
    tableName = "conversation",
    indices = [Index(value = ["packageName", "roomKey"], unique = true), Index("lastMessageAt")],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val packageName: String,
    val roomKey: String,
    val roomTitle: String,
    val isGroup: Boolean,
    val lastMessageAt: Long,
    val lastSender: String?,
    val lastText: String,
    /** 이 앱 안에서 아직 안 본 개수. 카톡·Teams의 읽음과는 무관. */
    val unreadCount: Int,
)

@Entity(
    tableName = "message",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["conversationId", "dedupKey"], unique = true), Index("receivedAt")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    /** 중복 제거 키. 메시지 고유번호가 있으면 `id:…`, 없으면 시각·보낸 사람·내용 조합. */
    val dedupKey: String,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val postedAt: Long,
    val receivedAt: Long,
    val isDeletedBySender: Boolean,
    /** 상대가 삭제한 메시지의 원문. */
    val originalText: String?,
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversation WHERE packageName = :packageName AND roomKey = :roomKey")
    suspend fun findConversation(packageName: String, roomKey: String): ConversationEntity?

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun getConversation(id: Long): ConversationEntity?

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM message WHERE conversationId = :conversationId AND dedupKey = :dedupKey")
    suspend fun findMessage(conversationId: Long, dedupKey: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("SELECT * FROM conversation ORDER BY lastMessageAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE id = :id")
    fun observeConversation(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY postedAt ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM message WHERE isMine = 0 AND receivedAt > :since")
    fun observeIncomingSince(since: Long): Flow<Int>

    @Query("UPDATE conversation SET unreadCount = 0 WHERE id = :id")
    suspend fun markSeen(id: Long)

    @Query("DELETE FROM message WHERE receivedAt < :before")
    suspend fun deleteMessagesBefore(before: Long)

    @Query("DELETE FROM conversation WHERE id NOT IN (SELECT DISTINCT conversationId FROM message)")
    suspend fun deleteEmptyConversations()
}
