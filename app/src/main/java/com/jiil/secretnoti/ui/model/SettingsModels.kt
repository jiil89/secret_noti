package com.jiil.secretnoti.ui.model

import androidx.annotation.StringRes
import com.jiil.secretnoti.R

/** 손잡이 크기. heightDp는 터치 영역 높이. */
enum class HandleSize(@StringRes val labelRes: Int, val heightDp: Int) {
    SMALL(R.string.settings_handle_size_small, 96),
    MEDIUM(R.string.settings_handle_size_medium, 120),
    LARGE(R.string.settings_handle_size_large, 160),
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
    /** 비밀번호를 받아 암호화한 백업 파일을 만든다. */
    fun backup(password: String)
    /** 백업 파일을 고른 뒤 비밀번호로 풀어서 합친다. */
    fun restore(password: String)
}
