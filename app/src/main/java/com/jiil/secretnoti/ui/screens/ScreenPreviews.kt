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

private val sampleApps = listOf(AppRef("com.kakao.talk", "카카오톡"), AppRef("com.microsoft.teams", "Teams"), AppRef("com.samsung.android.messaging", "문자"))

private fun sampleConversations() = listOf(
    ConversationUi(1, Source.KAKAO, "com.kakao.talk", "카카오톡", "엄마", false, "엄마", "주말에 김치 가지러 올 거지? 미리 말해줘", sampleAt(20, 52), 1, isPinned = true, hasKeyword = true),
    ConversationUi(2, Source.KAKAO, "com.kakao.talk", "카카오톡", "회사 동기 단톡방", true, "민수", "다음 주 금요일 회식 장소 투표 올렸어요", sampleAt(20, 47), 12, deletedCount = 2),
    ConversationUi(3, Source.TEAMS, "com.microsoft.teams", "Teams", "김팀장님", false, "김팀장님", "월요일 보고 자료 오전 중에 한번 볼 수 있을까요?", sampleAt(20, 30), 1),
    ConversationUi(4, Source.SMS, "com.samsung.android.messaging", "문자", "택배 알림", false, null, "[배송완료] 주문하신 상품이 문 앞에 도착했습니다.", sampleAt(19, 15), 1),
    ConversationUi(5, Source.KAKAO, "com.kakao.talk", "카카오톡", "대학 친구들", true, "지훈", "사진 3장을 보냈습니다", sampleAt(18, 2), 5, previewHidden = true),
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
    NotiTheme { EdgePanel(sampleConversations(), sampleApps, null, {}, {}, {}, {}, false) }
}

@Preview(name = "왼쪽 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun LeftPanelPreview() {
    NotiTheme { EdgePanel(sampleConversations(), sampleApps.take(2), null, {}, {}, {}, {}, true) }
}

@Preview(name = "빈 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun EmptyPanelPreview() {
    NotiTheme { EdgePanel(emptyList(), emptyList(), null, {}, {}, {}, {}, false) }
}

@Preview(name = "전체 대화 목록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ConversationListPreview() {
    NotiTheme { ConversationListScreen(sampleConversations(), sampleApps, null, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(name = "그룹 대화 기록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RoomPreview() {
    NotiTheme { RoomScreen("회사 동기 단톡방", "카카오톡", "com.kakao.talk", true, sampleMessages(), listOf("금요일", "장소"), {}, {}) }
}

@Preview(name = "큰 글자 · 문자", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
private fun LargeTextRoomPreview() {
    NotiTheme { RoomScreen("택배 알림", "문자", "com.samsung.android.messaging", false,
        listOf(MessageUi(1, "택배 알림", false, "[배송완료] 주문하신 상품이 문 앞에 도착했습니다.", sampleAt(19, 15), false)), emptyList(), {}, {}) }
}

@Preview(name = "손잡이 안내", showBackground = true)
@Composable
private fun HandleTooltipPreview() {
    NotiTheme { HandleTooltip() }
}

@Preview(name = "어두운 엣지패널", widthDp = 316, heightDp = 652)
@Composable
private fun DarkPanelPreview() {
    NotiTheme(ThemeMode.DARK) { EdgePanel(sampleConversations(), sampleApps, null, {}, {}, {}, {}, false) }
}

@Preview(name = "어두운 대화방", widthDp = 390, heightDp = 844)
@Composable
private fun DarkRoomPreview() {
    NotiTheme(ThemeMode.DARK) {
        RoomScreen("회사 동기 단톡방", "카카오톡", "com.kakao.talk", true, sampleMessages(), listOf("금요일"), {}, {})
    }
}

@Preview(name = "검색 결과", widthDp = 390, heightDp = 844)
@Composable
private fun SearchPreview() {
    NotiTheme {
        SearchScreen("금요일", {}, listOf(SearchResultUi(2, 1, "회사 동기 단톡방", "com.kakao.talk", "카카오톡",
            "민수", false, "다음 주 금요일 회식 장소 투표 올렸어요", sampleAt(20, 47), true)), listOf("장소"), {}, {})
    }
}

@Preview(name = "잠금", widthDp = 390, heightDp = 844)
@Composable
private fun LockPreview() { NotiTheme(ThemeMode.DARK) { LockScreen({}, "다시 시도해 주세요") } }

@Preview(name = "설정 · 권한 필요", widthDp = 390, heightDp = 1800)
@Composable
private fun SettingsPreview() {
    NotiTheme { SettingsScreen(sampleSettings(), PreviewSettingsActions) }
}

@Preview(name = "설정 · 어둡게 · 큰 글자", widthDp = 390, heightDp = 2100, fontScale = 1.3f)
@Composable
private fun DarkSettingsPreview() {
    NotiTheme(ThemeMode.DARK) {
        SettingsScreen(sampleSettings().copy(handleEnabled = false, themeMode = ThemeMode.DARK), PreviewSettingsActions)
    }
}

private fun sampleSettings() = SettingsUi(
    apps = listOf(AppSettingUi("com.kakao.talk", "카카오톡", true, "메시지 알림을 보낸 적 있음", true),
        AppSettingUi("com.samsung.android.messaging", "문자", true, "기본 문자 앱", true),
        AppSettingUi("com.example.app", "다른 앱", false, "설치됨", false)),
    handleEnabled = true, handleOnLeft = true, handlePosition = 0.35f, handleSize = HandleSize.MEDIUM,
    gestureExclusion = true, retentionDays = 30, permissions = PermissionSummaryUi(true, false, true),
    lockEnabled = false, lockAvailable = true, hidePreviews = true, deletionAlerts = true,
    notificationPermission = false, savePhotos = true, keywords = listOf("금요일", "장소"),
    hiddenConversations = sampleConversations().take(1), themeMode = ThemeMode.SYSTEM,
)

private object PreviewSettingsActions : SettingsActions {
    override fun back() {}
    override fun toggleApp(packageName: String, enabled: Boolean) {}
    override fun setHandleEnabled(enabled: Boolean) {}
    override fun setHandleSide(onLeft: Boolean) {}
    override fun setHandlePosition(position: Float) {}
    override fun setHandleSize(size: HandleSize) {}
    override fun setGestureExclusion(enabled: Boolean) {}
    override fun setRetention(days: Int) {}
    override fun openPermissions() {}
    override fun openDiagnostics() {}
    override fun setLock(enabled: Boolean) {}
    override fun setHidePreviews(enabled: Boolean) {}
    override fun setDeletionAlerts(enabled: Boolean) {}
    override fun requestNotificationPermission() {}
    override fun setSavePhotos(enabled: Boolean) {}
    override fun addKeyword(keyword: String) {}
    override fun removeKeyword(keyword: String) {}
    override fun unhideConversation(id: Long) {}
    override fun setThemeMode(mode: ThemeMode) {}
    override fun backup(password: String) {}
    override fun restore(password: String) {}
}
