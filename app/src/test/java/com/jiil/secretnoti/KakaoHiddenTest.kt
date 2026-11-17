package com.jiil.secretnoti

import com.jiil.secretnoti.capture.NotificationParser
import com.jiil.secretnoti.capture.NotificationSnapshot
import com.jiil.secretnoti.capture.RawMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoHiddenTest {
    private fun snap(
        title: String?, text: String?, messages: List<RawMessage> = emptyList(),
        pkg: String = "com.kakao.talk", category: String? = "msg", summary: Boolean = false,
    ) = NotificationSnapshot(
        packageName = pkg, tag = "1", shortcutId = null, locusId = null, postTime = 1L,
        isGroupSummary = summary, category = category, title = title, text = text, bigText = null, subText = null,
        conversationTitle = null, hiddenConversationTitle = null, isGroupConversation = null, selfKey = null,
        messages = messages,
    )

    @Test fun hiddenWhenTitleIsAppName() =
        assertTrue(NotificationParser.isKakaoContentHidden(snap("카카오톡", "새로운 메시지가 있습니다.")))

    @Test fun hiddenWhenTitleBlank() = assertTrue(NotificationParser.isKakaoContentHidden(snap(null, "메시지")))

    @Test fun hiddenWhenMessagesHaveNoSender() = assertTrue(NotificationParser.isKakaoContentHidden(
        snap("KakaoTalk", null, listOf(RawMessage("카카오톡", null, "새 메시지", 1L, null)))))

    @Test fun normalMessageIsNotHidden() = assertFalse(NotificationParser.isKakaoContentHidden(
        snap("엄마", "밥 먹었니", listOf(RawMessage("엄마", "k", "밥 먹었니", 1L, "9")))))

    @Test fun nonMessageKakaoNoticeIsIgnored() =
        assertFalse(NotificationParser.isKakaoContentHidden(snap("카카오톡", "업데이트 안내", category = null)))

    @Test fun summaryIsIgnored() =
        assertFalse(NotificationParser.isKakaoContentHidden(snap("카카오톡", null, summary = true)))

    @Test fun otherAppsAreIgnored() =
        assertFalse(NotificationParser.isKakaoContentHidden(snap("카카오톡", "x", pkg = "com.example")))
}
