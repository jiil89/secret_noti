package com.jiil.secretnoti.ui.model

import com.jiil.secretnoti.capture.Source

/** 손잡이 크기. heightDp는 터치 영역 높이. */
enum class HandleSize(val label: String, val heightDp: Int) {
    SMALL("작게", 96),
    MEDIUM("보통", 120),
    LARGE("크게", 160),
}

/** 보관 기간 선택지(일). 0은 기간 없이 계속 보관. */
val RETENTION_OPTIONS = listOf(7, 30, 90, 180, 365, 0)

data class SourceSettingUi(
    val source: Source,
    val packageName: String?,
    val enabled: Boolean,
    /** "설치됨", "기본 문자 앱: …" 같은 보조 설명. */
    val detail: String,
)

data class PermissionSummaryUi(
    val notificationAccess: Boolean,
    val overlay: Boolean,
    val batteryExempt: Boolean,
) {
    val allGranted: Boolean get() = notificationAccess && overlay && batteryExempt
}

data class SettingsUi(
    val sources: List<SourceSettingUi>,
    val handleEnabled: Boolean,
    val handleOnLeft: Boolean,
    /** 0f = 화면 맨 위, 1f = 맨 아래. */
    val handlePosition: Float,
    val handleSize: HandleSize,
    val gestureExclusion: Boolean,
    val retentionDays: Int,
    val permissions: PermissionSummaryUi,
)
