package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.jiil.secretnoti.R

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.NotiTheme
import com.jiil.secretnoti.ui.model.*
import java.time.LocalDate
import java.time.ZoneId

private fun sampleAt(hour: Int, minute: Int, daysAgo: Long = 0) =
    LocalDate.now().minusDays(daysAgo).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
private fun sampleApps() = listOf(AppRef("com.kakao.talk", stringResource(R.string.screen_preview_kakao)), AppRef("com.microsoft.teams", stringResource(R.string.screen_preview_teams)), AppRef("com.samsung.android.messaging", stringResource(R.string.screen_preview_sms)))

@Composable
private fun sampleConversations() = listOf(
    ConversationUi(6, Source.KAKAO, "com.kakao.talk", stringResource(R.string.screen_preview_kakao), stringResource(R.string.screen_preview_ad_title), false, null, stringResource(R.string.screen_preview_ad_message), sampleAt(20, 55), 1, looksLikeAd = true),
    ConversationUi(1, Source.KAKAO, "com.kakao.talk", stringResource(R.string.screen_preview_kakao), stringResource(R.string.screen_preview_mom), false, stringResource(R.string.screen_preview_mom), stringResource(R.string.screen_preview_mom_message), sampleAt(20, 52), 1, isPinned = true, hasKeyword = true),
    ConversationUi(2, Source.KAKAO, "com.kakao.talk", stringResource(R.string.screen_preview_kakao), stringResource(R.string.screen_preview_coworkers), true, stringResource(R.string.screen_preview_minsu), stringResource(R.string.screen_preview_poll), sampleAt(20, 47), 12, deletedCount = 2, senderCount = 3),
    ConversationUi(3, Source.TEAMS, "com.microsoft.teams", stringResource(R.string.screen_preview_teams), stringResource(R.string.screen_preview_manager), false, stringResource(R.string.screen_preview_manager), stringResource(R.string.screen_preview_report), sampleAt(20, 30), 1),
    ConversationUi(4, Source.SMS, "com.samsung.android.messaging", stringResource(R.string.screen_preview_sms), stringResource(R.string.screen_preview_delivery), false, null, stringResource(R.string.screen_preview_delivered), sampleAt(19, 15), 1),
    ConversationUi(5, Source.KAKAO, "com.kakao.talk", stringResource(R.string.screen_preview_kakao), stringResource(R.string.screen_preview_friends), true, stringResource(R.string.screen_preview_jihoon), pluralStringResource(R.plurals.screen_preview_photos, 3, 3), sampleAt(18, 2), 5, previewHidden = true),
)

@Composable
private fun sampleMutedConversations() = sampleConversations().filter { it.id == 6L || it.id == 4L }
    .map { it.copy(isMuted = true, hasRecords = it.id == 6L, unread = 0) }

@Composable
private fun sampleParticipants() = listOf(
    stringResource(R.string.screen_preview_minsu),
    stringResource(R.string.screen_preview_hyunwoo),
    stringResource(R.string.screen_preview_jiyoung),
)

@Composable
private fun sampleMessages() = listOf(
    MessageUi(1, stringResource(R.string.screen_preview_minsu), false, stringResource(R.string.screen_preview_invite), sampleAt(20, 31, 1), false),
    MessageUi(2, stringResource(R.string.screen_preview_jiyoung), false, stringResource(R.string.screen_preview_available), sampleAt(20, 33), false),
    MessageUi(3, stringResource(R.string.screen_preview_hyunwoo), false, stringResource(R.string.screen_preview_late), sampleAt(20, 35), true),
    MessageUi(4, null, true, stringResource(R.string.screen_preview_reply), sampleAt(20, 38), false),
    MessageUi(5, stringResource(R.string.screen_preview_minsu), false, stringResource(R.string.screen_preview_venue), sampleAt(20, 40), false),
    MessageUi(6, stringResource(R.string.screen_preview_minsu), false, stringResource(R.string.screen_preview_poll), sampleAt(20, 47), false),
)

@Preview(name = "오른쪽 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun EdgePanelPreview() {
    NotiTheme { EdgePanel(sampleConversations(), sampleApps(), null, {}, {}, {}, {}, false, 316.dp, 260.dp, 360.dp, {}, {}, {}, {}, { _, _ -> }) }
}

@Preview(name = "왼쪽 엣지패널", widthDp = 360, heightDp = 652, showBackground = true)
@Composable
private fun LeftPanelPreview() {
    NotiTheme { EdgePanel(sampleConversations(), sampleApps().take(2), null, {}, {}, {}, {}, true, 360.dp, 260.dp, 360.dp, {}, {}, {}, {}, { _, _ -> }) }
}

@Preview(name = "빈 엣지패널", widthDp = 316, heightDp = 652, showBackground = true)
@Composable
private fun EmptyPanelPreview() {
    NotiTheme { EdgePanel(emptyList(), emptyList(), null, {}, {}, {}, {}, false, 316.dp, 260.dp, 360.dp, {}, {}, {}, {}, { _, _ -> }) }
}

@Preview(name = "전체 대화 목록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ConversationListPreview() {
    NotiTheme { ConversationListScreen(sampleConversations().filter { it.id != 6L && it.id != 4L }, sampleApps(), null, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, sampleMutedConversations(), {}) }
}

@Preview(name = "그룹 대화 기록", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RoomPreview() {
    NotiTheme { RoomScreen(stringResource(R.string.screen_preview_coworkers), stringResource(R.string.screen_preview_kakao), "com.kakao.talk", true, sampleParticipants(), sampleMessages(), listOf(stringResource(R.string.screen_preview_friday), stringResource(R.string.screen_preview_place)), {}, {}, {}) }
}

@Preview(name = "큰 글자 · 문자", widthDp = 390, heightDp = 844, fontScale = 1.5f, showBackground = true)
@Composable
private fun LargeTextRoomPreview() {
    NotiTheme { RoomScreen(stringResource(R.string.screen_preview_delivery), stringResource(R.string.screen_preview_sms), "com.samsung.android.messaging", false, emptyList(),
        listOf(MessageUi(1, stringResource(R.string.screen_preview_delivery), false, stringResource(R.string.screen_preview_delivered), sampleAt(19, 15), false)), emptyList(), {}, {}, {}) }
}

@Preview(name = "손잡이 안내", showBackground = true)
@Composable
private fun HandleTooltipPreview() {
    NotiTheme { HandleTooltip() }
}

@Preview(name = "어두운 엣지패널", widthDp = 316, heightDp = 652)
@Composable
private fun DarkPanelPreview() {
    NotiTheme(ThemeMode.DARK) { EdgePanel(sampleConversations(), sampleApps(), null, {}, {}, {}, {}, false, 316.dp, 260.dp, 360.dp, {}, {}, {}, {}, { _, _ -> }) }
}

@Preview(name = "어두운 대화방", widthDp = 390, heightDp = 844)
@Composable
private fun DarkRoomPreview() {
    NotiTheme(ThemeMode.DARK) {
        RoomScreen(stringResource(R.string.screen_preview_coworkers), stringResource(R.string.screen_preview_kakao), "com.kakao.talk", true, sampleParticipants(), sampleMessages(), listOf(stringResource(R.string.screen_preview_friday)), {}, {}, {})
    }
}

@Preview(name = "검색 결과", widthDp = 390, heightDp = 844)
@Composable
private fun SearchPreview() {
    NotiTheme {
        SearchScreen(stringResource(R.string.screen_preview_friday), {}, listOf(SearchResultUi(2, 1, stringResource(R.string.screen_preview_coworkers), "com.kakao.talk", stringResource(R.string.screen_preview_kakao),
            stringResource(R.string.screen_preview_minsu), false, stringResource(R.string.screen_preview_poll), sampleAt(20, 47), true)), listOf(stringResource(R.string.screen_preview_place)), {}, {})
    }
}

@Preview(name = "잠금", widthDp = 390, heightDp = 844)
@Composable
private fun LockPreview() { NotiTheme(ThemeMode.DARK) { LockScreen({}, stringResource(R.string.screen_preview_retry)) } }

@Preview(name = "설정 · 권한 필요", widthDp = 390, heightDp = 1800)
@Composable
private fun SettingsPreview() {
    NotiTheme { SettingsScreen(sampleSettings(), PreviewSettingsActions) }
}

@Preview(name = "설정 · 어둡게 · 큰 글자", widthDp = 390, heightDp = 2100, fontScale = 1.3f)
@Composable
private fun DarkSettingsPreview() {
    NotiTheme(ThemeMode.DARK) {
        SettingsScreen(sampleSettings().copy(handleEnabled = false, themeMode = ThemeMode.DARK, panelWidthDp = 300, glowUsesAppColor = false), PreviewSettingsActions)
    }
}

@Composable
private fun sampleSettings() = SettingsUi(
    apps = listOf(AppSettingUi("com.kakao.talk", stringResource(R.string.screen_preview_kakao), true, stringResource(R.string.screen_preview_sent_notifications), true),
        AppSettingUi("com.samsung.android.messaging", stringResource(R.string.screen_preview_sms), true, stringResource(R.string.screen_preview_default_sms), true),
        AppSettingUi("com.example.app", stringResource(R.string.screen_preview_other_app), false, stringResource(R.string.screen_preview_installed), false)),
    handleEnabled = true, handleOnLeft = true, handlePosition = 0.35f, handleSize = HandleSize.MEDIUM,
    gestureExclusion = true, retentionDays = 30, permissions = PermissionSummaryUi(true, false, true),
    lockEnabled = false, lockAvailable = true, hidePreviews = true, deletionAlerts = true,
    notificationPermission = false, savePhotos = true, keywords = listOf(stringResource(R.string.screen_preview_friday), stringResource(R.string.screen_preview_place)),
    hiddenConversations = sampleConversations().take(1), themeMode = ThemeMode.SYSTEM,
    panelWidthDp = PanelWidth.NORMAL.dp,
    mutedConversations = sampleMutedConversations(),
    readElsewhereAction = ReadElsewhereAction.REMOVE_FROM_PANEL,
    autoMuteAds = false,
    indicatorBadge = true, indicatorGlow = true, glowUsesAppColor = true,
    glowColor = 0xFF2558E8.toInt(), peekBubble = false,
)

private object PreviewSettingsActions : SettingsActions {
    override fun back() {}
    override fun toggleApp(packageName: String, enabled: Boolean) {}
    override fun setHandleEnabled(enabled: Boolean) {}
    override fun setHandleSide(onLeft: Boolean) {}
    override fun setHandlePosition(position: Float) {}
    override fun setHandleSize(size: HandleSize) {}
    override fun setPanelWidth(dp: Int) {}
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
    override fun unmuteConversation(id: Long) {}
    override fun setReadElsewhereAction(action: ReadElsewhereAction) {}
    override fun setAutoMuteAds(enabled: Boolean) {}
    override fun setIndicatorBadge(enabled: Boolean) {}
    override fun setIndicatorGlow(enabled: Boolean) {}
    override fun setGlowUsesAppColor(enabled: Boolean) {}
    override fun setGlowColor(argb: Int) {}
    override fun setPeekBubble(enabled: Boolean) {}
    override fun previewGlow() {}
    override fun setThemeMode(mode: ThemeMode) {}
    override fun backup(password: String) {}
    override fun restore(password: String) {}
}

@Preview(name = "Stop saving dialog", widthDp = 390, heightDp = 600)
@Composable
private fun MuteDialogPreview() {
    NotiTheme {
        com.jiil.secretnoti.ui.components.MuteConversationDialog(
            stringResource(R.string.screen_preview_ad_title), {}, {})
    }
}

@Preview(name = "RGB glow picker", widthDp = 360, showBackground = true)
@Preview(name = "RGB glow picker - narrow", widthDp = 280, showBackground = true)
@Composable
private fun GlowPickerPreview() {
    NotiTheme {
        com.jiil.secretnoti.ui.components.GlowColorPicker(0xFF2558E8.toInt(), {})
    }
}

@Preview(name = "Peek bubble - right", showBackground = true)
@Composable
private fun PeekBubbleRightPreview() {
    NotiTheme {
        com.jiil.secretnoti.ui.components.PeekBubble(
            stringResource(R.string.screen_preview_mom), stringResource(R.string.screen_preview_mom_message),
            "com.kakao.talk", false, {})
    }
}

@Preview(name = "Peek bubble - left dark", showBackground = true)
@Composable
private fun PeekBubbleLeftPreview() {
    NotiTheme(ThemeMode.DARK) {
        com.jiil.secretnoti.ui.components.PeekBubble(
            stringResource(R.string.screen_preview_manager), stringResource(R.string.screen_preview_report),
            "com.microsoft.teams", true, {})
    }
}

@Preview(name = "Muted chats only", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MutedOnlyListPreview() {
    NotiTheme { ConversationListScreen(emptyList(), sampleApps(), null, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, sampleMutedConversations(), {}) }
}

@Preview(name = "Muted chats filtered", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun FilteredMutedListPreview() {
    NotiTheme(ThemeMode.DARK) { ConversationListScreen(emptyList(), sampleApps(), "com.samsung.android.messaging", {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, sampleMutedConversations(), {}) }
}
