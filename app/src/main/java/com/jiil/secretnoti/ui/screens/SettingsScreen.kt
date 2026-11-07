package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.NotiTheme
import com.jiil.secretnoti.ui.components.AppIcon
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUi,
    onBack: () -> Unit,
    onSourceToggle: (Source, Boolean) -> Unit,
    onHandleEnabledChange: (Boolean) -> Unit,
    onHandleSideChange: (onLeft: Boolean) -> Unit,
    onHandlePositionChange: (Float) -> Unit,
    onHandleSizeChange: (HandleSize) -> Unit,
    onGestureExclusionChange: (Boolean) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    var pendingRetention by remember(state.retentionDays) { mutableStateOf<Int?>(null) }
    val position = state.handlePosition.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f

    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(NotiSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(NotiSpacing.button)) {
                    Icon(NotiIcons.chevronLeft, contentDescription = "뒤로", tint = NotiColors.Body)
                }
                Text("설정", style = MaterialTheme.typography.titleLarge, color = NotiColors.Text)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(NotiSpacing.content),
                verticalArrangement = Arrangement.spacedBy(NotiSpacing.panel),
            ) {
                SettingsPermissions(state.permissions, onOpenPermissions)
                SettingsSection("대상 앱") {
                    state.sources.forEach { source ->
                        SettingsSwitchRow(
                            label = source.source.label,
                            detail = source.detail,
                            checked = source.enabled,
                            onCheckedChange = { onSourceToggle(source.source, it) },
                            leading = {
                                val packageName = source.packageName
                                if (packageName != null) {
                                    AppIcon(packageName, 24.dp)
                                } else {
                                    Icon(NotiIcons.message, null, Modifier.size(24.dp), tint = NotiColors.Secondary)
                                }
                            },
                        )
                    }
                    SettingsHelper("켠 앱의 알림만 저장하고 보여줘요. 끄면 기존 기록은 숨겨지고 지워지지 않아요.")
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection("엣지 손잡이") {
                    SettingsSwitchRow("손잡이 표시", checked = state.handleEnabled,
                        onCheckedChange = onHandleEnabledChange)
                    Text("표시 방향", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(true, false).forEachIndexed { index, onLeft ->
                            SegmentedButton(
                                selected = state.handleOnLeft == onLeft,
                                onClick = { onHandleSideChange(onLeft) },
                                enabled = state.handleEnabled,
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                                icon = {},
                            ) { Text(if (onLeft) "왼쪽" else "오른쪽") }
                        }
                    }
                    Surface(shape = NotiShapes.banner, color = NotiColors.DetailBackground) {
                        Row(
                            Modifier.fillMaxWidth().padding(NotiSpacing.content),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NotiSpacing.content),
                        ) {
                            HandlePreview(state.handleOnLeft, position, state.handleSize, state.handleEnabled)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("손잡이 미리보기", style = MaterialTheme.typography.labelLarge)
                                SettingsHelper("갤럭시 엣지 패널 손잡이와 겹치지 않는 위치로 맞춰 주세요.")
                            }
                        }
                    }
                    Text("세로 위치", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = position,
                        onValueChange = onHandlePositionChange,
                        valueRange = 0f..1f,
                        enabled = state.handleEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
                            .semantics { contentDescription = "손잡이 세로 위치, 위에서 아래로" },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SettingsHelper("위")
                        SettingsHelper("아래")
                    }
                    Text("손잡이 크기", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        HandleSize.entries.forEachIndexed { index, handleSize ->
                            SegmentedButton(
                                selected = state.handleSize == handleSize,
                                onClick = { onHandleSizeChange(handleSize) },
                                enabled = state.handleEnabled,
                                shape = SegmentedButtonDefaults.itemShape(index, HandleSize.entries.size),
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                                icon = {},
                            ) { Text(handleSize.label) }
                        }
                    }
                    SettingsSwitchRow(
                        label = "뒤로 가기 제스처와 겹치지 않게",
                        detail = "손잡이를 밀 때 뒤로 가기가 실행되면 켜 두세요",
                        checked = state.gestureExclusion,
                        onCheckedChange = onGestureExclusionChange,
                        enabled = state.handleEnabled,
                    )
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection("보관 기간") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(NotiSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(NotiSpacing.tiny),
                    ) {
                        RETENTION_OPTIONS.forEach { days ->
                            FilterChip(
                                selected = state.retentionDays == days,
                                onClick = {
                                    if (days != state.retentionDays) {
                                        if (requiresRetentionConfirmation(state.retentionDays, days)) {
                                            pendingRetention = days
                                        } else {
                                            onRetentionChange(days)
                                        }
                                    }
                                },
                                label = { Text(when (days) { 0 -> "계속 보관"; 365 -> "1년"; else -> "${days}일" }) },
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                            )
                        }
                    }
                    SettingsHelper("기간이 지난 메시지는 자동으로 지워져요.")
                }
                TextButton(onClick = onOpenDiagnostics,
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                    Text("진단 도구 (알림 덤프·제스처 기록)", color = NotiColors.Secondary)
                }
            }
        }
    }

    pendingRetention?.let { days ->
        AlertDialog(
            onDismissRequest = { pendingRetention = null },
            title = { Text("보관 기간을 줄일까요?") },
            text = { Text("${days}일보다 오래된 메시지가 지금 지워져요. 계속할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRetention = null
                    onRetentionChange(days)
                }, modifier = Modifier.heightIn(min = NotiSpacing.button)) { Text("지우기") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRetention = null },
                    modifier = Modifier.heightIn(min = NotiSpacing.button)) { Text("취소") }
            },
        )
    }
}

private fun requiresRetentionConfirmation(current: Int, next: Int): Boolean =
    next != 0 && (current == 0 || next < current)

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiSpacing.medium)) {
        Text(title, modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium, color = NotiColors.Text)
        content()
    }
}

@Composable
private fun SettingsHelper(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = NotiColors.Secondary)
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = NotiSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotiSpacing.medium),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NotiSpacing.tiny)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = NotiColors.Text)
            if (!detail.isNullOrBlank()) SettingsHelper(detail)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SettingsPermissions(permissions: PermissionSummaryUi, onOpenPermissions: () -> Unit) {
    val missing = buildList {
        if (!permissions.notificationAccess) add("알림 접근")
        if (!permissions.overlay) add("다른 앱 위에 표시")
        if (!permissions.batteryExempt) add("배터리 최적화 제외")
    }
    Surface(
        shape = NotiShapes.banner,
        color = if (permissions.allGranted) NotiColors.DetailBackground else NotiColors.NoticeBackground,
    ) {
        Column(Modifier.fillMaxWidth().padding(NotiSpacing.content),
            verticalArrangement = Arrangement.spacedBy(NotiSpacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(NotiSpacing.small),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(NotiIcons.info, null, Modifier.size(20.dp),
                    tint = if (permissions.allGranted) NotiColors.Secondary else NotiColors.NoticeForeground)
                Text(if (permissions.allGranted) "권한 모두 허용됨" else "권한 설정이 필요해요",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (permissions.allGranted) NotiColors.Body else NotiColors.NoticeForeground)
            }
            if (!permissions.allGranted) {
                Text("허용 필요: ${missing.joinToString(" / ")}",
                    style = MaterialTheme.typography.bodyMedium, color = NotiColors.NoticeForeground)
                TextButton(onClick = onOpenPermissions, modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                    Text("권한 설정", color = NotiColors.NoticeForeground)
                }
            }
        }
    }
}

@Composable
private fun HandlePreview(onLeft: Boolean, position: Float, handleSize: HandleSize, enabled: Boolean) {
    val description = if (enabled) {
        "${if (onLeft) "왼쪽" else "오른쪽"} 손잡이, 위에서 ${(position * 100).toInt()}%, ${handleSize.label}"
    } else "손잡이 숨김"
    Canvas(Modifier.size(56.dp, 104.dp).semantics { contentDescription = description }) {
        val inset = 2.dp.toPx()
        val phoneSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawRoundRect(NotiColors.Background, Offset(inset, inset), phoneSize, CornerRadius(10.dp.toPx()))
        drawRoundRect(NotiColors.Secondary, Offset(inset, inset), phoneSize,
            CornerRadius(10.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        if (enabled) {
            val barHeight = phoneSize.height * handleSize.heightDp / 800f
            val barWidth = 3.dp.toPx()
            val x = if (onLeft) inset else size.width - inset - barWidth
            val y = inset + (phoneSize.height - barHeight) * position
            drawRoundRect(NotiColors.Accent, Offset(x, y), Size(barWidth, barHeight), CornerRadius(barWidth))
        }
    }
}

@Preview(name = "설정 · 권한 필요 · 왼쪽 손잡이", widthDp = 390, heightDp = 1800, showBackground = true)
@Composable
private fun SettingsMissingPermissionPreview() {
    SettingsPreview(handleEnabled = true, permissions = PermissionSummaryUi(true, false, true))
}

@Preview(name = "설정 · 손잡이 끔 · 큰 글자", widthDp = 390, heightDp = 2100, fontScale = 1.3f, showBackground = true)
@Composable
private fun SettingsDisabledHandlePreview() {
    SettingsPreview(handleEnabled = false, permissions = PermissionSummaryUi(true, true, true))
}

@Composable
private fun SettingsPreview(handleEnabled: Boolean, permissions: PermissionSummaryUi) {
    NotiTheme {
        SettingsScreen(
            state = SettingsUi(
                sources = listOf(
                    SourceSettingUi(Source.KAKAO, "com.kakao.talk", true, "설치됨"),
                    SourceSettingUi(Source.SMS, null, true, "기본 문자 앱"),
                    SourceSettingUi(Source.TEAMS, "com.microsoft.teams", false, "개인 프로필의 알림"),
                ),
                handleEnabled = handleEnabled, handleOnLeft = true, handlePosition = 0.35f,
                handleSize = HandleSize.MEDIUM, gestureExclusion = true, retentionDays = 30,
                permissions = permissions,
            ),
            onBack = {}, onSourceToggle = { _, _ -> }, onHandleEnabledChange = {},
            onHandleSideChange = {}, onHandlePositionChange = {}, onHandleSizeChange = {},
            onGestureExclusionChange = {}, onRetentionChange = {}, onOpenPermissions = {},
            onOpenDiagnostics = {},
        )
    }
}
