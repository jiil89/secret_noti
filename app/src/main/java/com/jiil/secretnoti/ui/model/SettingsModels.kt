package com.jiil.secretnoti.ui.model

import androidx.annotation.StringRes
import com.jiil.secretnoti.R

/** 손잡이 크기. heightDp는 터치 영역 높이. */
enum class HandleSize(@StringRes val labelRes: Int, val heightDp: Int) {
    SMALL(R.string.settings_handle_size_small, 96),
    MEDIUM(R.string.settings_handle_size_medium, 120),
    LARGE(R.string.settings_handle_size_large, 160),
}

/** 엣지패널 폭 프리셋(dp). 패널 가장자리를 끌어서 그 사이 값으로도 맞출 수 있다. */
enum class PanelWidth(@StringRes val labelRes: Int, val dp: Int) {
    NARROW(R.string.settings_panel_width_narrow, 280),
    NORMAL(R.string.settings_panel_width_normal, 316),
    WIDE(R.string.settings_panel_width_wide, 360);

    companion object {
        const val MIN_DP = 260
        /** 바깥을 눌러 닫을 자리로 화면 폭에서 이만큼은 남긴다. */
        const val SCREEN_MARGIN_DP = 48
        fun of(dp: Int): PanelWidth? = entries.firstOrNull { it.dp == dp }
    }
}

/** 손잡이 새 메시지 배지 모양. */
enum class BadgeStyle(@StringRes val labelRes: Int) {
    NUMBER(R.string.settings_badge_style_number),
    DOT(R.string.settings_badge_style_dot),
}

/** 보관 기간 선택지(일). 0은 기간 없이 계속 보관. */
val RETENTION_OPTIONS = listOf(7, 30, 90, 180, 365, 0)

data class AppSettingUi(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    /** "메시지 알림을 보낸 적 있음", "기본 문자 앱" 같은 보조 설명. */
    val detail: String,
    /** 알려진 메신저이거나 메시지 알림을 보낸 적이 있어 위쪽에 보여줄 앱. */
    val suggested: Boolean,
)

data class PermissionSummaryUi(
    val notificationAccess: Boolean,
    val overlay: Boolean,
    val batteryExempt: Boolean,
) {
    val allGranted: Boolean get() = notificationAccess && overlay && batteryExempt
}

data class SettingsUi(
    /** 추천 앱(suggested)이 먼저, 나머지는 "다른 앱 더 보기"를 눌렀을 때 보여준다. */
    val apps: List<AppSettingUi>,
    val handleEnabled: Boolean,
    val handleOnLeft: Boolean,
    /** 0f = 화면 맨 위, 1f = 맨 아래. */
    val handlePosition: Float,
    val handleSize: HandleSize,
    val gestureExclusion: Boolean,
    val retentionDays: Int,
    val permissions: PermissionSummaryUi,
    /** 앱 잠금(지문·얼굴·화면 잠금 PIN) 사용. */
    val lockEnabled: Boolean,
    /** 폰에 화면 잠금이 설정돼 있어 앱 잠금을 쓸 수 있다. */
    val lockAvailable: Boolean,
    /** 엣지패널·위젯에서 메시지 내용 가리기. */
    val hidePreviews: Boolean,
    /** 상대가 메시지를 삭제하면 알림으로 알려주기. */
    val deletionAlerts: Boolean,
    /** 알림 표시 권한(Android 13+). 삭제 알림에 필요. */
    val notificationPermission: Boolean,
    /** 사진 미리보기 저장. */
    val savePhotos: Boolean,
    val keywords: List<String>,
    val hiddenConversations: List<ConversationUi>,
    val themeMode: ThemeMode,
    /** 엣지패널 폭(dp). 프리셋 값이 아니면 사용자가 끌어서 맞춘 폭. */
    val panelWidthDp: Int,
    /** 저장을 끈 대화방 (다시 켤 수 있게 보여준다). */
    val mutedConversations: List<ConversationUi>,
    val readElsewhereAction: ReadElsewhereAction,
    /** 광고로 보이는 새 대화방은 처음부터 저장하지 않기. */
    val autoMuteAds: Boolean,
    /** 새 메시지 표시: 손잡이 숫자 배지. */
    val indicatorBadge: Boolean,
    /** 배지 모양: 숫자 또는 점. */
    val badgeStyle: BadgeStyle,
    /** 새 메시지 표시: 손잡이 반짝임. */
    val indicatorGlow: Boolean,
    /** 반짝임 색을 보낸 앱 색으로 (false면 [glowColor]). */
    val glowUsesAppColor: Boolean,
    /** 직접 고른 반짝임 색 (ARGB). */
    val glowColor: Int,
    /** 새 메시지 표시: 손잡이 옆 미리보기 말풍선. */
    val peekBubble: Boolean,
)

/** 설정 화면에서 일어나는 모든 조작. */
interface SettingsActions {
    fun back()
    fun toggleApp(packageName: String, enabled: Boolean)
    fun setHandleEnabled(enabled: Boolean)
    fun setHandleSide(onLeft: Boolean)
    fun setHandlePosition(position: Float)
    fun setHandleSize(size: HandleSize)
    fun setGestureExclusion(enabled: Boolean)
    fun setRetention(days: Int)
    fun openPermissions()
    fun openDiagnostics()
    fun setLock(enabled: Boolean)
    fun setHidePreviews(enabled: Boolean)
    fun setDeletionAlerts(enabled: Boolean)
    fun requestNotificationPermission()
    fun setSavePhotos(enabled: Boolean)
    fun addKeyword(keyword: String)
    fun removeKeyword(keyword: String)
    fun unhideConversation(id: Long)
    fun setThemeMode(mode: ThemeMode)
    fun setPanelWidth(dp: Int)
    fun unmuteConversation(id: Long)
    fun setReadElsewhereAction(action: ReadElsewhereAction)
    fun setAutoMuteAds(enabled: Boolean)
    fun setIndicatorBadge(enabled: Boolean)
    fun setBadgeStyle(style: BadgeStyle)
    fun setIndicatorGlow(enabled: Boolean)
    fun setGlowUsesAppColor(enabled: Boolean)
    /** ARGB. 알파는 무시하고 불투명으로 쓴다. */
    fun setGlowColor(argb: Int)
    fun setPeekBubble(enabled: Boolean)
    /** 지금 설정으로 손잡이를 한 번 반짝여 본다. */
    fun previewGlow()
    /** 비밀번호를 받아 암호화한 백업 파일을 만든다. */
    fun backup(password: String)
    /** 백업 파일을 고른 뒤 비밀번호로 풀어서 합친다. */
    fun restore(password: String)
}
