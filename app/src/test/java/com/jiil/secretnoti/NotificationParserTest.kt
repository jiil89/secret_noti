package com.jiil.secretnoti

import com.jiil.secretnoti.capture.KakaoEmoticons
import com.jiil.secretnoti.capture.NotificationParser
import com.jiil.secretnoti.capture.NotificationSnapshot
import com.jiil.secretnoti.capture.RawMessage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    /** 기기에서 덤프한 JSON(NotificationDumper 형식)을 스냅샷으로 되돌린다. */
    private fun snapshotFromDump(j: JSONObject): NotificationSnapshot {
        val e = j.getJSONObject("extras")
        fun str(o: JSONObject, k: String) = if (o.has(k) && !o.isNull(k)) o.get(k).toString() else null
        val messages = e.optJSONArray("android.messages")?.let { arr ->
            (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                val person = m.optJSONObject("sender_person")
                RawMessage(
                    sender = person?.let { str(it, "name") } ?: str(m, "sender"),
                    senderKey = person?.let { str(it, "key") },
                    text = m.getString("text"),
                    time = m.getLong("time"),
                    messageId = m.optJSONObject("extras")?.let { str(it, "chatLogId") },
                )
            }
        } ?: emptyList()
        return NotificationSnapshot(
            packageName = j.getString("package"),
            tag = str(j, "tag"),
            shortcutId = str(j, "shortcutId"),
            locusId = str(j, "locusId"),
            postTime = j.getLong("postTime"),
            isGroupSummary = j.getBoolean("isGroupSummary"),
            title = str(e, "android.title"),
            text = str(e, "android.text"),
            bigText = str(e, "android.bigText"),
            subText = str(e, "android.subText"),
            conversationTitle = str(e, "android.conversationTitle"),
            hiddenConversationTitle = str(e, "android.hiddenConversationTitle"),
            isGroupConversation = if (e.has("android.isGroupConversation")) e.getBoolean("android.isGroupConversation") else null,
            selfKey = e.optJSONObject("android.messagingUser")?.let { str(it, "key") },
            messages = messages,
        )
    }

    private fun fixture(name: String): List<NotificationSnapshot> {
        val text = javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()
        val arr = JSONArray(text)
        return (0 until arr.length()).map { snapshotFromDump(arr.getJSONObject(it)) }
    }

    @Test
    fun kakaoGroupSummaryIsIgnored() {
        val summary = fixture("kakao_1to1.json")[0]
        assertTrue(summary.isGroupSummary)
        assertNull(NotificationParser.parse(summary))
    }

    @Test
    fun kakaoOneToOneUsesChatIdAsRoomKeyAndChatLogIdForDedup() {
        val parsed = NotificationParser.parse(fixture("kakao_1to1.json")[1])!!
        assertEquals("1000000000001", parsed.roomKey)
        assertEquals("상대방", parsed.roomTitle)
        assertFalse(parsed.isGroup)
        assertEquals(listOf("ㅎㅎ", "ㅎ"), parsed.messages.map { it.text })
        assertEquals("id:3968845044553160705", parsed.messages[0].dedupKey)
        assertTrue(parsed.messages.none { it.isMine })
        assertEquals("상대방", parsed.messages[0].sender)
    }

    private fun base(messages: List<RawMessage>) = NotificationSnapshot(
        packageName = "com.kakao.talk", tag = "77", shortcutId = "77", locusId = null, postTime = 1000,
        isGroupSummary = false, title = "가족방", text = "엄마: 밥 먹자", bigText = null, subText = null,
        conversationTitle = "가족방", hiddenConversationTitle = null, isGroupConversation = true,
        selfKey = "me", messages = messages,
    )

    @Test
    fun kakaoNumberIsAppUnreadCount() {
        val msgs = listOf(RawMessage("엄마", "mom", "밥 먹자", 10, "1"))
        assertEquals(3, NotificationParser.parse(base(msgs).copy(number = 3))!!.appUnread)
        assertNull(NotificationParser.parse(base(msgs).copy(number = 0))!!.appUnread)
        // 카톡 외 앱의 number는 뜻이 제각각이라 쓰지 않는다.
        assertNull(NotificationParser.parse(base(msgs).copy(packageName = "com.microsoft.teams", number = 3))!!.appUnread)
    }

    @Test
    fun groupConversationKeepsSendersAndMarksMine() {
        val parsed = NotificationParser.parse(
            base(
                listOf(
                    RawMessage("엄마", "mom", "밥 먹자", 10, "1"),
                    RawMessage(null, null, "곧 갈게", 20, "2"),
                    RawMessage("나", "me", "PC에서 보냄", 30, "3"),
                ),
            ),
        )!!
        assertTrue(parsed.isGroup)
        assertEquals("가족방", parsed.roomTitle)
        assertEquals(listOf(false, true, true), parsed.messages.map { it.isMine })
        assertEquals("엄마", parsed.messages[0].sender)
        assertNull(parsed.messages[1].sender)
    }

    @Test
    fun withoutMessageIdDedupKeyIsStableAcrossUpdates() {
        val m = RawMessage("엄마", null, "밥 먹자", 10, null)
        val first = NotificationParser.parse(base(listOf(m)))!!.messages[0].dedupKey
        val again = NotificationParser.parse(base(listOf(m)).copy(postTime = 9999))!!.messages[0].dedupKey
        assertEquals(first, again)
    }

    @Test
    fun plainNotificationFallsBackToBigText() {
        val parsed = NotificationParser.parse(
            base(emptyList()).copy(
                isGroupConversation = false, conversationTitle = null, title = "택배",
                text = "[배송완료] 문 앞…", bigText = "[배송완료] 문 앞에 도착했습니다.",
            ),
        )!!
        assertEquals("택배", parsed.roomTitle)
        assertEquals("[배송완료] 문 앞에 도착했습니다.", parsed.messages.single().text)
    }

    @Test
    fun detectsDeletionMarkers() {
        assertTrue(NotificationParser.isDeletionMarker("삭제된 메시지입니다."))
        assertFalse(NotificationParser.isDeletionMarker("메시지 삭제 기능 써봤어?"))
    }

    @Test
    fun middleRemovalIsDeletionButSlidingWindowIsNot() {
        // 오래된 것부터 빠지는 건 삭제가 아님
        assertEquals(emptyList<String>(), NotificationParser.removedFromMiddle(listOf("a", "b", "c"), listOf("b", "c", "d")))
        // 가운데 b가 빠지면 삭제
        assertEquals(listOf("b"), NotificationParser.removedFromMiddle(listOf("a", "b", "c"), listOf("a", "c", "d")))
    }

    @Test
    fun otherAppsOnlyAcceptMessageNotifications() {
        val promo = base(emptyList()).copy(isGroupConversation = false, conversationTitle = null, category = "promo")
        assertNull(NotificationParser.parse(promo, messagesOnly = true))
        val msg = promo.copy(category = "msg")
        assertTrue(NotificationParser.parse(msg, messagesOnly = true) != null)
        assertNull(NotificationParser.parse(msg.copy(isOngoing = true)))
    }

    @Test
    fun keepsPhotoUriForImageMessages() {
        val parsed = NotificationParser.parse(
            base(listOf(RawMessage("엄마", "mom", "사진을 보냈습니다.", 10, "9", "image/", "content://x/1"))),
        )!!
        assertEquals("content://x/1", parsed.messages.single().imageUri)
    }

    @Test
    fun kakaoMiniEmoticonsBecomeEmojiOrTags() {
        val text = "형님 생일 축하합니더 🥳🥳(축하해)(축하해)(손하트)(토끼)(뿌잉)"
        assertEquals("형님 생일 축하합니더 🥳🥳🎉🎉🫰🐰(뿌잉)", KakaoEmoticons.toDisplay(text))
        assertEquals(KakaoEmoticons.Part.Tag("뿌잉"), KakaoEmoticons.split(text).last())
        // 날짜·회사 표기는 건드리지 않는다
        assertEquals("11/8(토) 10시, (주)카카오", KakaoEmoticons.toDisplay("11/8(토) 10시, (주)카카오"))
    }

    @Test
    fun kakaoStickerUriWithoutMimeIsKept() {
        val parsed = NotificationParser.parse(
            base(listOf(RawMessage("친구", "m", "이모티콘을 보냈습니다.", 10, "7", null, "content://kakao/emoticon_dir/83/x"))),
        )!!
        assertEquals("content://kakao/emoticon_dir/83/x", parsed.messages.single().imageUri)
    }

    @Test
    fun detectsKoreanAdMessages() {
        assertTrue(NotificationParser.looksLikeAd("(광고) 오늘만 50% 할인"))
        assertTrue(NotificationParser.looksLikeAd("  [광고] 신상품 안내"))
        assertFalse(NotificationParser.looksLikeAd("광고 봤어? 웃기더라"))
        // 삼성 메시지 실제 알림: MMS 제목, [Web발신], 보이지 않는 방향 글자(U+200E)
        assertTrue(NotificationParser.looksLikeAd("<제목:\u200E (광고)나비잠기저귀\u200E 핫딜(지마켓)>\u200E\n[Web발신] (광고)더자람"))
        assertTrue(NotificationParser.looksLikeAd("<제목:\u200E 태미안의원입니다.>\u200E\n[Web발신] [광고] (광고)상봉역"))
        assertTrue(NotificationParser.looksLikeAd("<제목:\u200E [LG라이프케어]>\u200E\n[Web발신] (광고) [LG라이프케어] 쇼핑지원금"))
        assertTrue(NotificationParser.looksLikeAd("[Web발신]\n(광고) 할인 안내"))
        assertFalse(NotificationParser.looksLikeAd("[Web발신] [남양주도시공사] 예매취소 (광고) 아님"))
        assertFalse(NotificationParser.looksLikeAd("<제목: 회의 자료>\n오늘 회의는 (광고) 건입니다"))
    }

    @Test
    fun kakaoGroupUsesShortcutLabelAndKeepsKnownTitleOtherwise() {
        // 실제 카톡 단톡 알림: isGroupConversation=true, conversationTitle 없음, title = 마지막 보낸 사람
        val group = base(
            listOf(RawMessage("사촌", "a", "생일 축하해", 10, "1"), RawMessage("친구", "b", "축하해요", 20, "2")),
        ).copy(conversationTitle = null, title = "친구", isGroupConversation = true)
        val named = NotificationParser.parse(group.copy(shortcutLabel = "가족 모임"))!!
        assertEquals("가족 모임", named.roomTitle)
        assertTrue(named.titleReliable)
        val unnamed = NotificationParser.parse(group)!!
        assertEquals("친구", unnamed.roomTitle)
        assertFalse(unnamed.titleReliable)
        // 앱이 단톡 표시를 안 해도 보낸 사람이 둘 이상이면 단톡방
        assertTrue(NotificationParser.parse(group.copy(isGroupConversation = false))!!.isGroup)
    }

    @Test
    fun instagramOneToOneUsesPeerNotOwnAccountName() {
        // 실제 인스타 DM 알림 구조: conversationTitle·hiddenConversationTitle = 내 계정, isGroupConversation=false
        val dm = NotificationSnapshot(
            packageName = "com.instagram.android", tag = "direct|1;thread_id:9", shortcutId = null, locusId = null,
            postTime = 1000, isGroupSummary = false, category = "msg", title = "my.account: 친구", text = "안녕", bigText = null,
            subText = null, conversationTitle = "my.account", hiddenConversationTitle = "my.account",
            isGroupConversation = false, selfKey = "me", messages = listOf(RawMessage("친구", "peer", "안녕", 10, null)),
        )
        val parsed = NotificationParser.parse(dm, messagesOnly = true)!!
        assertFalse(parsed.isGroup)
        assertTrue(parsed.groupExplicit)
        assertEquals("친구", parsed.roomTitle)
        assertEquals("direct|1;thread_id:9", parsed.roomKey)
    }

    @Test
    fun kakaoChannelIsDetectedByMissingReplyAction() {
        // 실기기: 사람 대화는 "읽음·답장", 채널·알림톡은 "읽음"만 있다
        val person = base(listOf(RawMessage("엄마", "mom", "밥 먹자", 10, "1"))).copy(canReply = true)
        val alimtalk = base(listOf(RawMessage("카드사", "biz", "[카드사] 이용 안내", 10, "2"))).copy(
            canReply = false, isGroupConversation = false, conversationTitle = null, title = "카드사",
        )
        assertFalse(NotificationParser.parse(person)!!.isChannel)
        assertTrue(NotificationParser.parse(alimtalk)!!.isChannel)
        // 카톡이 아닌 앱은 답장 버튼이 없어도 채널로 보지 않는다
        assertFalse(NotificationParser.parse(alimtalk.copy(packageName = "com.example.app"))!!.isChannel)
    }
}
