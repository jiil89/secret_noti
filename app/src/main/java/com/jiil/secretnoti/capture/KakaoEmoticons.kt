package com.jiil.secretnoti.capture

/**
 * 카톡 미니 이모티콘은 알림 글자에 "(축하해)(손하트)"처럼 이름으로만 온다 (그림은 알림에 없음).
 * 잘 알려진 이름은 비슷한 이모지로 바꾸고, 모르는 이름은 화면에서 작은 꼬리표로 보여준다.
 */
object KakaoEmoticons {
    /** 괄호 안 1~8자의 한글·영문 이름. 공백·숫자가 들어간 "(주)", "(11/8)" 같은 건 대부분 제외된다. */
    val PATTERN = Regex("""\(([가-힣A-Za-z]{1,8})\)""")

    private val known = mapOf(
        "축하" to "🎉", "축하해" to "🎉", "축하해요" to "🎉", "생일" to "🎂", "케이크" to "🎂", "파티" to "🥳", "선물" to "🎁",
        "하트" to "❤️", "사랑" to "🥰", "사랑해" to "🥰", "손하트" to "🫰", "뽀뽀" to "😘", "윙크" to "😉",
        "웃음" to "😄", "하하" to "😆", "미소" to "🙂", "크크" to "😆", "부끄" to "😊", "헤롱" to "😵",
        "울음" to "😭", "눈물" to "😢", "흑흑" to "😢", "화남" to "😠", "짜증" to "😤", "놀람" to "😲", "깜짝" to "😲", "헉" to "😱",
        "최고" to "👍", "굿" to "👍", "좋아" to "👍", "엄지척" to "👍", "오케이" to "👌", "박수" to "👏", "감사" to "🙏", "감사해" to "🙏",
        "미안" to "🙇", "힘내" to "💪", "화이팅" to "💪", "파이팅" to "💪", "졸려" to "😪", "잘자" to "🌙", "메롱" to "😜",
        "토끼" to "🐰", "고양이" to "🐱", "강아지" to "🐶", "곰" to "🐻", "꽃" to "🌸", "별" to "⭐", "해" to "☀️",
    )

    /** 미리보기·검색 결과용: 아는 이름만 이모지로 바꾼 글자. */
    fun toDisplay(text: String): String =
        PATTERN.replace(text) { m -> known[m.groupValues[1]] ?: m.value }

    /** 말풍선용 조각: 글자, 이모지로 바꾼 이름, 모르는 이름(꼬리표로 그림). */
    sealed interface Part {
        data class Plain(val text: String) : Part
        data class Emoji(val emoji: String) : Part
        data class Tag(val name: String) : Part
    }

    fun split(text: String): List<Part> = buildList {
        var last = 0
        for (m in PATTERN.findAll(text)) {
            if (m.range.first > last) add(Part.Plain(text.substring(last, m.range.first)))
            val name = m.groupValues[1]
            add(known[name]?.let { Part.Emoji(it) } ?: Part.Tag(name))
            last = m.range.last + 1
        }
        if (last < text.length) add(Part.Plain(text.substring(last)))
    }

    fun isSticker(text: String) = text.trim().removeSuffix(".") == "이모티콘을 보냈습니다"
}
