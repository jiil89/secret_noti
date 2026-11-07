package com.jiil.secretnoti.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.NotiTheme
import com.jiil.secretnoti.ui.model.*
import java.time.LocalDate
import java.time.ZoneId

private fun sampleAt(hour: Int, minute: Int, daysAgo: Long = 0) =
    LocalDate.now().minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun sampleConversations() = listOf(
    ConversationUi(1, Source.KAKAO, "com.kakao.talk", "엄마", false, "엄마", "주말에 김치 가지러 올 거지? 미리 말해줘", sampleAt(20, 52), 1),
    ConversationUi(2, Source.KAKAO, "com.kakao.talk", "회사 동기 단톡방", true, "민수", "다음 주 금요일 회식 장소 투표 올렸어요", sampleAt(20, 47), 12),
    ConversationUi(3, Source.TEAMS, "com.microsoft.teams", "김팀장님", false, "김팀장님", "월요일 보고 자료 오전 중에 한번 볼 수 있을까요?", sampleAt(20, 30), 1),
    ConversationUi(4, Source.SMS, "com.samsung.android.messaging", "택배 알림", false, null, "[배송완료] 주문하신 상품이 문 앞에 도착했습니다.", sampleAt(19, 15), 1),
    ConversationUi(5, Source.KAKAO, "com.kakao.talk", "대학 친구들", true, "지훈", "사진 3장을 보냈습니다", sampleAt(18, 2), 5),
)

private fun sampleMessages() = listOf(
    MessageUi(1, "민수", false, "다들 다음 주 금요일 시간 돼요?", sampleAt(20, 31, 1), false),
    MessageUi(2, "지영", false, "저 가능해요!", sampleAt(20, 33), false),
    MessageUi(3, "현우", false, "저 그날 반차라 조금 늦을 수도 있어요", sampleAt(20, 35), true),
    MessageUi(4, null, true, "저도 가능해요. 장소 정해지면 알려주세요.", sampleAt(20, 38), false),
    MessageUi(5, "민수", false, "장소는 강남역이랑 판교 중에 고르면 될 듯", sampleAt(20, 40), false),
    MessageUi(6, "민수", false, "다음 주 금요일 회식 장소 투표 올렸어요", sampleAt(20, 47), false),
)

@Preview(name = "오른쪽 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun EdgePanelPreview() {
    NotiTheme { EdgePanel(sampleConversations(), Source.entries.toSet(), SourceFilter.ALL, {}, {}, {}, {}, false) }
}

@Preview(name = "왼쪽 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun LeftPanelPreview() {
    NotiTheme { EdgePanel(sampleConversations(), setOf(Source.KAKAO, Source.SMS), SourceFilter.ALL, {}, {}, {}, {}, true) }
}

@Preview(name = "빈 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun EmptyPanelPreview() {
    NotiTheme { EdgePanel(emptyList(), emptySet(), SourceFilter.ALL, {}, {}, {}, {}, false) }
}

@Preview(name = "전체 대화 목록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ConversationListPreview() {
    NotiTheme { ConversationListScreen(sampleConversations(), Source.entries.toSet(), SourceFilter.ALL, {}, {}, {}) }
}

@Preview(name = "그룹 대화 기록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RoomPreview() {
    NotiTheme { RoomScreen("회사 동기 단톡방", Source.KAKAO, "com.kakao.talk", true, sampleMessages(), {}, {}) }
}

@Preview(name = "큰 글자 · 문자", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
private fun LargeTextRoomPreview() {
    NotiTheme { RoomScreen("택배 알림", Source.SMS, "com.samsung.android.messaging", false,
        listOf(MessageUi(1, "택배 알림", false, "[배송완료] 주문하신 상품이 문 앞에 도착했습니다.", sampleAt(19, 15), false)), {}, {}) }
}

@Preview(name = "손잡이 안내", showBackground = true)
@Composable
private fun HandleTooltipPreview() {
    NotiTheme { HandleTooltip() }
}
