package com.jiil.secretnoti

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
}
