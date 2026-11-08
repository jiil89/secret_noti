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
    /** 저장 끔: 이 방의 새 알림은 저장하지 않는다. 방 자체는 기억해 둔다. */
    @ColumnInfo(defaultValue = "0") val isMuted: Boolean = false,
    /** 엣지패널에서 뺀 시각. 그 뒤에 온 메시지가 있을 때만 패널에 다시 보인다. */
    @ColumnInfo(defaultValue = "0") val panelDismissedAt: Long = 0,
    /** "(광고)"로 시작하는 메시지를 받은 적 있음. */
    @ColumnInfo(defaultValue = "0") val looksLikeAd: Boolean = false,
    /** 이 방을 마지막으로 "읽은" 시각: 앱에서 열었거나, 패널에서 뺐거나, 원래 앱에서 읽었을 때. 손잡이 배지 계산용. */
    @ColumnInfo(defaultValue = "0") val readAt: Long = 0,
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
    /** 이 방에서 저장된 서로 다른 보낸 사람 수 (실제 인원수 아님). */
    val senderCount: Int,
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
        SELECT c.*, (SELECT COUNT(*) FROM message m WHERE m.conversationId = c.id AND m.isDeletedBySender = 1) AS deletedCount,
            (SELECT COUNT(DISTINCT m2.sender) FROM message m2 WHERE m2.conversationId = c.id AND m2.isMine = 0
                AND m2.sender IS NOT NULL) AS senderCount
        FROM conversation c ORDER BY c.isPinned DESC, c.lastMessageAt DESC
        """,
    )
    fun observeConversations(): Flow<List<ConversationWithStats>>

    @Query(
        """
        SELECT c.*, (SELECT COUNT(*) FROM message m WHERE m.conversationId = c.id AND m.isDeletedBySender = 1) AS deletedCount,
            (SELECT COUNT(DISTINCT m2.sender) FROM message m2 WHERE m2.conversationId = c.id AND m2.isMine = 0
                AND m2.sender IS NOT NULL) AS senderCount
        FROM conversation c WHERE c.id = :id
        """,
    )
    fun observeConversation(id: Long): Flow<ConversationWithStats?>

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY postedAt ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    /** 이 방에서 메시지를 보낸 사람들, 최근에 보낸 순. */
    @Query(
        """
        SELECT sender FROM message WHERE conversationId = :conversationId AND isMine = 0 AND sender IS NOT NULL
        GROUP BY sender ORDER BY MAX(postedAt) DESC
        """,
    )
    fun observeSenders(conversationId: Long): Flow<List<String>>

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

    /** 손잡이 배지: 마지막으로 패널을 연 뒤 온 메시지 중, 그 방을 그 뒤에 읽지 않은 것만 센다. */
    @Query(
        """
        SELECT COUNT(*) FROM message m JOIN conversation c ON c.id = m.conversationId
        WHERE m.isMine = 0 AND m.receivedAt > :since AND m.receivedAt > c.readAt
            AND c.isMuted = 0 AND c.isHidden = 0
        """,
    )
    fun observeIncomingSince(since: Long): Flow<Int>

    @Query("UPDATE conversation SET unreadCount = 0, readAt = MAX(readAt, :now) WHERE id = :id")
    suspend fun markSeen(id: Long, now: Long)

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

    // 저장을 끈 방은 메시지가 없어도 남겨 둔다 (다시 와도 저장하지 않으려고).
    @Query("DELETE FROM conversation WHERE isMuted = 0 AND id NOT IN (SELECT DISTINCT conversationId FROM message)")
    suspend fun deleteEmptyConversations()

    @Query("UPDATE conversation SET isMuted = :muted WHERE id = :id")
    suspend fun setMuted(id: Long, muted: Boolean)

    @Query("DELETE FROM message WHERE conversationId = :conversationId")
    suspend fun deleteMessagesOf(conversationId: Long)

    @Query("UPDATE conversation SET lastText = '', lastSender = NULL, unreadCount = 0 WHERE id = :id")
    suspend fun clearSummary(id: Long)

    @Query("UPDATE conversation SET panelDismissedAt = :at, readAt = MAX(readAt, :at) WHERE id = :id")
    suspend fun setPanelDismissedAt(id: Long, at: Long)

    @Query("UPDATE conversation SET panelDismissedAt = :at, readAt = MAX(readAt, :at) WHERE lastMessageAt <= :at")
    suspend fun dismissAllFromPanel(at: Long)

    /** 다른 앱에서 읽음: 안 읽은 수·배지 0, [dismissAt]이 0보다 크면 패널에서도 뺀다. */
    @Query(
        """
        UPDATE conversation SET unreadCount = 0, readAt = MAX(readAt, :now),
            panelDismissedAt = MAX(panelDismissedAt, :dismissAt)
        WHERE packageName = :packageName AND roomKey = :roomKey
        """,
    )
    suspend fun markReadElsewhere(packageName: String, roomKey: String, now: Long, dismissAt: Long)

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
