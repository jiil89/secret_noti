package com.jiil.secretnoti.capture

/** 알림에서 뽑아낸 값만 담은 순수 데이터. 안드로이드 클래스에 의존하지 않아 JVM 단위 테스트가 가능하다. */
data class NotificationSnapshot(
    val packageName: String,
    val tag: String?,
    val shortcutId: String?,
    val locusId: String?,
    val postTime: Long,
    val isGroupSummary: Boolean,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val conversationTitle: String?,
    val hiddenConversationTitle: String?,
    val isGroupConversation: Boolean?,
    val selfKey: String?,
    val messages: List<RawMessage>,
)

/** MessagingStyle 메시지 하나. sender가 null이면 내가 보낸 메시지다. */
data class RawMessage(
    val sender: String?,
    val senderKey: String?,
    val text: String,
    val time: Long,
    /** 앱이 붙여 주는 메시지 고유번호. 카톡은 `chatLogId`. */
    val messageId: String?,
)

data class ParsedMessage(
    val dedupKey: String,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val postedAt: Long,
)

data class ParsedNotification(
    val roomKey: String,
    val roomTitle: String,
    val isGroup: Boolean,
    val messages: List<ParsedMessage>,
)

object NotificationParser {

    private val deletionMarkers = listOf(
        "삭제된 메시지입니다",
        "메시지가 삭제되었습니다",
        "이 메시지는 삭제되었습니다",
        "This message was deleted",
        "This message has been deleted",
    )

    fun isDeletionMarker(text: String): Boolean = deletionMarkers.any { text.contains(it) }

    /**
     * 그룹 요약 알림이나 내용 없는 알림이면 null.
     * 방 키는 shortcutId → locusId → tag → 방 이름 순으로 고른다 (카톡은 shortcutId = 채팅방 번호).
     */
    fun parse(s: NotificationSnapshot): ParsedNotification? {
        if (s.isGroupSummary) return null

        val isGroup = s.isGroupConversation ?: (s.conversationTitle != null)
        val roomTitle = if (isGroup) {
            s.conversationTitle ?: s.hiddenConversationTitle ?: s.subText ?: s.title
        } else {
            s.hiddenConversationTitle ?: s.title
        }?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val roomKey = s.shortcutId ?: s.locusId ?: s.tag ?: "title:$roomTitle"

        val messages = if (s.messages.isNotEmpty()) {
            s.messages.map { m ->
                val mine = m.sender == null || (m.senderKey != null && m.senderKey == s.selfKey)
                ParsedMessage(
                    dedupKey = m.messageId?.let { "id:$it" } ?: "t:${m.time}|${m.sender.orEmpty()}|${m.text}",
                    sender = if (mine) null else m.sender,
                    isMine = mine,
                    text = m.text,
                    postedAt = m.time,
                )
            }
        } else {
            val body = (s.bigText ?: s.text)?.takeIf { it.isNotBlank() } ?: return null
            listOf(
                ParsedMessage(
                    dedupKey = "p:${s.postTime}|$body",
                    sender = if (isGroup) s.title else roomTitle,
                    isMine = false,
                    text = body,
                    postedAt = s.postTime,
                ),
            )
        }
        return ParsedNotification(roomKey, roomTitle, isGroup, messages)
    }
}
