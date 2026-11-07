package com.jiil.secretnoti.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** M1 디버그용: 대상 앱 알림을 extras 전체와 함께 그대로 보관한다. 파서를 확정할 샘플. */
@Entity(tableName = "notification_dump", indices = [Index("receivedAt")])
data class NotificationDump(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val event: String,
    val packageName: String,
    val postTime: Long,
    val receivedAt: Long,
    val title: String?,
    val text: String?,
    val json: String,
)

@Dao
interface DumpDao {
    @Insert
    suspend fun insert(dump: NotificationDump): Long

    @Query("SELECT * FROM notification_dump ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NotificationDump>>

    @Query("SELECT * FROM notification_dump WHERE event = 'posted' AND title IS NOT NULL ORDER BY id DESC LIMIT :limit")
    fun observeRecentPosted(limit: Int): Flow<List<NotificationDump>>

    @Query("SELECT COUNT(*) FROM notification_dump")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_dump WHERE event = 'posted' AND title IS NOT NULL AND receivedAt > :since")
    fun observePostedSince(since: Long): Flow<Int>

    @Query("SELECT * FROM notification_dump ORDER BY id ASC")
    suspend fun all(): List<NotificationDump>

    @Query("DELETE FROM notification_dump")
    suspend fun clear()

    @Query("DELETE FROM notification_dump WHERE id <= (SELECT MAX(id) FROM notification_dump) - :keep")
    suspend fun trim(keep: Int)
}

@Database(
    entities = [NotificationDump::class, ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dumpDao(): DumpDao
    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "noti.db").build()
    }
}
