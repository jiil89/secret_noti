package com.jiil.secretnoti.capture

/** 알림에서 뽑아낸 값만 담은 순수 데이터. 안드로이드 클래스에 의존하지 않아 JVM 단위 테스트가 가능하다. */
data class NotificationSnapshot(
    val packageName: String,
    val tag: String?,
    val shortcutId: String?,
    val locusId: String?,
    val postTime: Long,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean = false,
    val category: String? = null,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val conversationTitle: String?,
    val hiddenConversationTitle: String?,
    val isGroupConversation: Boolean?,
    val selfKey: String?,
    val messages: List<RawMessage>,
    /** 알림에 붙은 대화 바로가기의 이름. 카톡 단톡방은 알림에 방 이름이 없고 여기에만 있다. */
    val shortcutLabel: String? = null,
)

/** MessagingStyle 메시지 하나. sender가 null이면 내가 보낸 메시지다. */
data class RawMessage(
    val sender: String?,
    val senderKey: String?,
    val text: String,
    val time: Long,
    /** 앱이 붙여 주는 메시지 고유번호. 카톡은 `chatLogId`. */
    val messageId: String?,
    /** 메시지에 붙은 데이터(사진 미리보기 등)의 MIME 타입과 주소. */
    val dataMimeType: String? = null,
    val dataUri: String? = null,
)

data class ParsedMessage(
    val dedupKey: String,
    val sender: String?,
    val isMine: Boolean,
    val text: String,
    val postedAt: Long,
    /** 사진 미리보기 주소(content://…). 앱이 알림에 넣어 준 경우에만. */
    val imageUri: String? = null,
)

data class ParsedNotification(
    val roomKey: String,
    val roomTitle: String,
    val isGroup: Boolean,
    val messages: List<ParsedMessage>,
    /** false면 방 이름을 몰라 마지막 보낸 사람 이름을 임시로 쓴 것 (기존 방 이름을 덮어쓰지 않는다). */
    val titleReliable: Boolean = true,
)

object NotificationParser {

    private val deletionMarkers = listOf(
        "삭제된 메시지입니다",
        "메시지가 삭제되었습니다",
        "이 메시지는 삭제되었습니다",
        "This message was deleted",
        "This message has been deleted",
        "You deleted this message",
        "메시지 전송이 취소되었습니다",
        "메시지를 보내지 않았습니다",
        "unsent a message",
    )

    fun isDeletionMarker(text: String): Boolean = deletionMarkers.any { text.contains(it) }

    /** 정보통신망법상 광고 메시지는 "(광고)"로 시작해야 한다. */
    fun looksLikeAd(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith("(광고)") || t.startsWith("[광고]") || t.startsWith("(AD)", ignoreCase = true)
    }

    /**
     * 같은 알림이 갱신될 때 목록 "중간"에서 빠진 메시지를 찾는다.
     * 카톡 같은 앱은 알림에 최근 몇 개만 담고 오래된 것부터 빼므로, 앞쪽에서 빠진 것은 삭제가 아니다.
     * 빠진 메시지보다 앞선 메시지가 아직 남아 있으면 중간에서 빠진 것 → 상대가 삭제한 것으로 본다.
     */
    fun removedFromMiddle(previous: List<String>, current: List<String>): List<String> {
        val now = current.toSet()
        return previous.filterIndexed { i, key ->
            key !in now && previous.subList(0, i).any { it in now }
        }
    }

    /**
     * 그룹 요약 알림이나 내용 없는 알림이면 null.
     * 방 키는 shortcutId → locusId → tag → 방 이름 순으로 고른다 (카톡은 shortcutId = 채팅방 번호).
     * [messagesOnly]가 true면 메시지형 알림(MessagingStyle 또는 메시지 카테고리)만 받는다. 일반 앱의 광고·상태 알림을 거르기 위함.
     */
    fun parse(s: NotificationSnapshot, messagesOnly: Boolean = false): ParsedNotification? {
        if (s.isGroupSummary || s.isOngoing) return null
        if (messagesOnly && s.messages.isEmpty() && s.category != "msg") return null

        // 보낸 사람이 둘 이상이면 단톡방으로 본다 (앱이 단톡 표시를 빼먹어도).
        val distinctSenders = s.messages.mapNotNull { m -> m.senderKey ?: m.sender }.filter { it != s.selfKey }.toSet()
        val isGroup = s.isGroupConversation == true || s.conversationTitle != null || distinctSenders.size >= 2
        fun String?.clean() = this?.trim()?.takeIf { it.isNotEmpty() }
        val namedTitle = if (isGroup) {
            s.conversationTitle.clean() ?: s.shortcutLabel.clean() ?: s.hiddenConversationTitle.clean() ?: s.subText.clean()
        } else {
            s.hiddenConversationTitle.clean()
        }
        // 카톡 단톡방은 title이 "마지막 보낸 사람"이라 방 이름으로 믿을 수 없다.
        val roomTitle = namedTitle ?: s.title.clean() ?: return null
        val titleReliable = namedTitle != null || !isGroup

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
                    // 카톡 큰 이모티콘은 MIME 없이 주소만 온다. 그림이 아니면 저장할 때 걸러진다.
                    imageUri = m.dataUri?.takeIf { m.dataMimeType == null || m.dataMimeType.startsWith("image/") },
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
        return ParsedNotification(roomKey, roomTitle, isGroup, messages, titleReliable)
    }
}
