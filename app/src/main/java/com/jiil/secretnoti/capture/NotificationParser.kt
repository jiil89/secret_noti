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
    /** 알림에서 바로 답장할 수 있는가 (RemoteInput 버튼이 있음). */
    val canReply: Boolean = true,
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
    /** 앱이 알림에 1:1/단톡을 직접 밝혔는가. 밝혔으면 그 값을 따른다 (예전에 잘못 단톡으로 저장된 방을 바로잡기 위함). */
    val groupExplicit: Boolean = false,
    /** 카톡 채널·알림톡 (사람이 아닌 기업·서비스가 보낸 대화방). */
    val isChannel: Boolean = false,
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

    /**
     * 카톡 채널·알림톡: 사람 대화(1:1·단톡)는 알림에 "답장" 버튼이 있고, 채널·알림톡은 "읽음"만 있다.
     * 실기기 알림 88건에서 이 기준으로 사람 11개 방·채널 13개 방이 100% 갈렸다.
     */
    fun isKakaoChannel(s: NotificationSnapshot): Boolean = s.packageName == "com.kakao.talk" && !s.canReply

    private val AD_MARKERS = listOf("(광고)", "[광고]", "(AD)", "[AD]")
    private val SENDER_TAGS = listOf("[Web발신]", "[국제발신]", "[국외발신]", "[해외발신]")

    /** 보이지 않는 방향 표시 글자(U+200E, U+2068 등)를 뺀다. 삼성 메시지가 번호·제목에 넣는다. */
    fun stripInvisible(text: String): String =
        text.filterNot { Character.getType(it) == Character.FORMAT.toInt() }

    /**
     * 정보통신망법상 광고 메시지는 "(광고)"로 시작해야 한다.
     * 문자는 앞에 "<제목: …>"(MMS 제목), "[Web발신]" 같은 꼬리표가 붙어 오므로 그것들을 건너뛰고 본다.
     * 예: "<제목: (광고)핫딜>\n[Web발신] (광고)…", "[Web발신] [광고] (광고)…"
     */
    fun looksLikeAd(text: String): Boolean {
        var t = stripInvisible(text)
        repeat(4) {
            t = t.trimStart()
            if (AD_MARKERS.any { t.startsWith(it, ignoreCase = true) }) return true
            t = when {
                t.startsWith("<제목:") -> {
                    val subject = t.removePrefix("<제목:").trimStart()
                    if (AD_MARKERS.any { subject.startsWith(it, ignoreCase = true) }) return true
                    t.substringAfter('>', "")
                }
                SENDER_TAGS.any { t.startsWith(it) } -> t.substringAfter(']')
                else -> return false
            }
        }
        return false
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
        val isGroup = (s.isGroupConversation ?: (s.conversationTitle != null)) || distinctSenders.size >= 2
        fun String?.clean() = this?.let(::stripInvisible)?.trim()?.takeIf { it.isNotEmpty() }
        val namedTitle = if (isGroup) {
            s.conversationTitle.clean() ?: s.shortcutLabel.clean() ?: s.hiddenConversationTitle.clean() ?: s.subText.clean()
        } else {
            // 1:1은 상대가 보낸 마지막 메시지의 보낸 사람이 가장 정확하다.
            // (인스타그램은 hiddenConversationTitle·conversationTitle에 "내" 계정 이름을 넣는다)
            s.messages.lastOrNull { it.sender != null && (it.senderKey == null || it.senderKey != s.selfKey) }?.sender.clean()
                ?: s.hiddenConversationTitle.clean()
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
        return ParsedNotification(
            roomKey, roomTitle, isGroup, messages, titleReliable,
            groupExplicit = s.isGroupConversation != null && distinctSenders.size < 2,
            isChannel = isKakaoChannel(s),
        )
    }
}
