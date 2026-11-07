package com.jiil.secretnoti.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
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
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isHidden: Boolean = false,
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
    /** 앱 안에 저장한 사진 미리보기 파일 경로. */
    @ColumnInfo(defaultValue = "NULL") val imagePath: String? = null,
)

/** 메시지 알림을 보낸 적 있는 앱. 설정의 대상 앱 추천에 쓴다. */
@Entity(tableName = "seen_app")
data class SeenAppEntity(
    @PrimaryKey val packageName: String,
    val lastSeenAt: Long,
)

data class ConversationWithStats(
    @Embedded val conversation: ConversationEntity,
    val deletedCount: Int,
)

data class SearchRow(
    @Embedded val message: MessageEntity,
    val roomTitle: String,
    val packageName: String,
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

    @Query("UPDATE message SET imagePath = :path WHERE id = :id")
    suspend fun setImagePath(id: Long, path: String)

    @Query(
        """
        SELECT c.*, (SELECT COUNT(*) FROM message m WHERE m.conversationId = c.id AND m.isDeletedBySender = 1) AS deletedCount
        FROM conversation c ORDER BY c.isPinned DESC, c.lastMessageAt DESC
        """,
    )
    fun observeConversations(): Flow<List<ConversationWithStats>>

    @Query(
        """
        SELECT c.*, (SELECT COUNT(*) FROM message m WHERE m.conversationId = c.id AND m.isDeletedBySender = 1) AS deletedCount
        FROM conversation c WHERE c.id = :id
        """,
    )
    fun observeConversation(id: Long): Flow<ConversationWithStats?>

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY postedAt ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT DISTINCT conversationId FROM message WHERE text LIKE '%' || :keyword || '%' ESCAPE '\\'")
    fun observeConversationsWithKeyword(keyword: String): Flow<List<Long>>

    @Query(
        """
        SELECT m.*, c.roomTitle AS roomTitle, c.packageName AS packageName FROM message m
        JOIN conversation c ON c.id = m.conversationId
        WHERE c.isHidden = 0 AND (
            m.text LIKE '%' || :query || '%' ESCAPE '\' OR
            m.originalText LIKE '%' || :query || '%' ESCAPE '\' OR
            m.sender LIKE '%' || :query || '%' ESCAPE '\' OR
            c.roomTitle LIKE '%' || :query || '%' ESCAPE '\'
        )
        ORDER BY m.postedAt DESC LIMIT 200
        """,
    )
    fun search(query: String): Flow<List<SearchRow>>

    @Query("SELECT COUNT(*) FROM message WHERE isMine = 0 AND receivedAt > :since")
    fun observeIncomingSince(since: Long): Flow<Int>

    @Query("UPDATE conversation SET unreadCount = 0 WHERE id = :id")
    suspend fun markSeen(id: Long)

    @Query("UPDATE conversation SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long)

    @Query("UPDATE conversation SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("SELECT imagePath FROM message WHERE conversationId = :conversationId AND imagePath IS NOT NULL")
    suspend fun imagePathsOf(conversationId: Long): List<String>

    @Query("SELECT imagePath FROM message WHERE receivedAt < :before AND imagePath IS NOT NULL")
    suspend fun imagePathsBefore(before: Long): List<String>

    @Query("DELETE FROM message WHERE receivedAt < :before")
    suspend fun deleteMessagesBefore(before: Long)

    @Query("DELETE FROM conversation WHERE id NOT IN (SELECT DISTINCT conversationId FROM message)")
    suspend fun deleteEmptyConversations()

    @Upsert
    suspend fun upsertSeenApp(app: SeenAppEntity)

    @Query("SELECT * FROM seen_app ORDER BY lastSeenAt DESC")
    fun observeSeenApps(): Flow<List<SeenAppEntity>>

    // 백업용
    @Query("SELECT * FROM conversation")
    suspend fun allConversations(): List<ConversationEntity>

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY postedAt ASC, id ASC")
    suspend fun messagesOf(conversationId: Long): List<MessageEntity>
}
