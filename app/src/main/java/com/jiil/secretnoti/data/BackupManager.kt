package com.jiil.secretnoti.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jiil.secretnoti.capture.PhotoSaver
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException : IOException()

/**
 * 대화 기록 백업·복원. 파일은 비밀번호로 암호화한다 (PBKDF2-SHA256 → AES-256-GCM).
 * 형식: "SNB1" + salt(16) + iv(12) + 암호화된 zip(data.json, images/…).
 * 인터넷을 쓰지 않으므로 파일은 사용자가 고른 위치(내 파일, USB 등)에만 저장된다.
 */
class BackupManager(private val context: Context, private val db: AppDatabase) {
    private val dao = db.chatDao()

    /** 백업을 쓰고 저장한 메시지 수를 돌려준다. */
    suspend fun export(uri: Uri, password: String): Int {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        }
        var count = 0
        val out: OutputStream = context.contentResolver.openOutputStream(uri) ?: throw IOException("cannot open")
        out.use { raw ->
            raw.write(MAGIC)
            raw.write(salt)
            raw.write(iv)
            ZipOutputStream(CipherOutputStream(raw, cipher)).use { zip ->
                val conversations = JSONArray()
                for (c in dao.allConversations()) {
                    val messages = JSONArray()
                    for (m in dao.messagesOf(c.id)) {
                        val imageName = m.imagePath?.let(::File)?.takeIf { it.exists() }?.let { file ->
                            val name = "images/${m.id}.jpg"
                            zip.putNextEntry(ZipEntry(name))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            name
                        }
                        messages.put(
                            JSONObject()
                                .put("dedupKey", m.dedupKey)
                                .put("sender", m.sender ?: JSONObject.NULL)
                                .put("isMine", m.isMine)
                                .put("text", m.text)
                                .put("postedAt", m.postedAt)
                                .put("receivedAt", m.receivedAt)
                                .put("isDeletedBySender", m.isDeletedBySender)
                                .put("originalText", m.originalText ?: JSONObject.NULL)
                                .put("image", imageName ?: JSONObject.NULL),
                        )
                        count++
                    }
                    conversations.put(
                        JSONObject()
                            .put("source", c.source)
                            .put("packageName", c.packageName)
                            .put("roomKey", c.roomKey)
                            .put("roomTitle", c.roomTitle)
                            .put("isGroup", c.isGroup)
                            .put("isPinned", c.isPinned)
                            .put("isHidden", c.isHidden)
                            .put("messages", messages),
                    )
                }
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(
                    JSONObject().put("version", 1).put("exportedAt", System.currentTimeMillis())
                        .put("conversations", conversations).toString().toByteArray(),
                )
                zip.closeEntry()
            }
        }
        return count
    }

    /** 백업을 기존 기록에 합친다. 이미 있는 메시지는 건너뛴다. 새로 들어온 메시지 수를 돌려준다. */
    suspend fun import(uri: Uri, password: String): Int {
        val temp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            var json: String? = null
            val input: InputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("cannot open")
            input.use { raw ->
                val data = DataInputStream(raw)
                val magic = ByteArray(4).also(data::readFully)
                if (!magic.contentEquals(MAGIC)) throw IOException("not a backup")
                val salt = ByteArray(16).also(data::readFully)
                val iv = ByteArray(12).also(data::readFully)
                val cipher = Cipher.getInstance(TRANSFORM).apply {
                    init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
                }
                try {
                    ZipInputStream(CipherInputStream(data, cipher)).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            if (entry.name == "data.json") {
                                json = zip.readBytes().decodeToString()
                            } else if (entry.name.startsWith("images/") && !entry.name.contains("..")) {
                                File(temp, entry.name.substringAfter("images/")).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                } catch (e: IOException) {
                    // GCM 인증 실패 = 비밀번호가 다르거나 파일이 손상됨.
                    throw WrongPasswordException()
                }
            }
            val root = JSONObject(json ?: throw WrongPasswordException())
            return merge(root.getJSONArray("conversations"), temp)
        } finally {
            temp.deleteRecursively()
        }
    }

    private suspend fun merge(conversations: JSONArray, images: File): Int {
        var added = 0
        val copies = mutableListOf<Triple<Long, Long, File>>()
        db.withTransaction {
            for (i in 0 until conversations.length()) {
                val c = conversations.getJSONObject(i)
                val pkg = c.getString("packageName")
                val roomKey = c.getString("roomKey")
                val existing = dao.findConversation(pkg, roomKey)
                val convId = existing?.id ?: dao.insertConversation(
                    ConversationEntity(
                        source = c.getString("source"),
                        packageName = pkg,
                        roomKey = roomKey,
                        roomTitle = c.getString("roomTitle"),
                        isGroup = c.getBoolean("isGroup"),
                        lastMessageAt = 0,
                        lastSender = null,
                        lastText = "",
                        unreadCount = 0,
                        isPinned = c.optBoolean("isPinned"),
                        isHidden = c.optBoolean("isHidden"),
                    ),
                )
                var latest: MessageEntity? = null
                val messages = c.getJSONArray("messages")
                for (j in 0 until messages.length()) {
                    val m = messages.getJSONObject(j)
                    val entity = MessageEntity(
                        conversationId = convId,
                        dedupKey = m.getString("dedupKey"),
                        sender = m.optString("sender").takeIf { !m.isNull("sender") },
                        isMine = m.getBoolean("isMine"),
                        text = m.getString("text"),
                        postedAt = m.getLong("postedAt"),
                        receivedAt = m.getLong("receivedAt"),
                        isDeletedBySender = m.getBoolean("isDeletedBySender"),
                        originalText = m.optString("originalText").takeIf { !m.isNull("originalText") },
                    )
                    val id = dao.insertMessage(entity)
                    if (id == -1L) continue
                    added++
                    if (latest == null || entity.postedAt >= latest.postedAt) latest = entity
                    if (!m.isNull("image")) {
                        val file = File(images, m.getString("image").substringAfter("images/"))
                        if (file.exists()) copies += Triple(convId, id, file)
                    }
                }
                val conv = dao.getConversation(convId)!!
                if (latest != null && latest.postedAt >= conv.lastMessageAt) {
                    dao.updateConversation(
                        conv.copy(lastMessageAt = latest.postedAt, lastSender = latest.sender, lastText = latest.text),
                    )
                }
            }
        }
        for ((convId, msgId, file) in copies) {
            val target = File(PhotoSaver.imageDir(context), "$convId-$msgId.jpg")
            file.copyTo(target, overwrite = true)
            dao.setImagePath(msgId, target.absolutePath)
        }
        return added
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private companion object {
        val MAGIC = "SNB1".toByteArray()
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
