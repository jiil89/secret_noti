package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.components.AppIcon
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: SettingsUi, actions: SettingsActions) {
    var expandedApps by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var passwordDialog by remember { mutableStateOf<Boolean?>(null) }
    var pendingRetention by remember(state.retentionDays) { mutableStateOf<Int?>(null) }
    val position = state.handlePosition.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f

    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(NotiSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions::back, modifier = Modifier.size(NotiSpacing.button)) {
                    Icon(NotiIcons.chevronLeft, contentDescription = "뒤로", tint = NotiColors.Body)
                }
                Text("설정", style = MaterialTheme.typography.titleLarge, color = NotiColors.Text)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(NotiSpacing.content),
                verticalArrangement = Arrangement.spacedBy(NotiSpacing.panel),
            ) {
                SettingsPermissions(state.permissions, actions::openPermissions)
                SettingsSection("대상 앱") {
                    val suggested = state.apps.filter { it.suggested }
                    val others = state.apps.filterNot { it.suggested }
                    (suggested + if (expandedApps) others else emptyList()).forEach { app ->
                        SettingsSwitchRow(app.label, app.enabled,
                            onCheckedChange = { actions.toggleApp(app.packageName, it) }, detail = app.detail,
                            leading = { AppIcon(app.packageName, 24.dp) })
                    }
                    if (others.isNotEmpty()) TextButton(onClick = { expandedApps = !expandedApps }) {
                        Text(if (expandedApps) "다른 앱 접기" else "다른 앱 더 보기 (${others.size})")
                    }
                    SettingsHelper("메시지 알림을 보내는 앱이면 대부분 쓸 수 있어요. 켠 앱의 알림만 저장해요.")
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection("엣지 손잡이") {
                    SettingsSwitchRow("손잡이 표시", checked = state.handleEnabled,
                        onCheckedChange = actions::setHandleEnabled)
                    Text("표시 방향", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(true, false).forEachIndexed { index, onLeft ->
                            SegmentedButton(
                                selected = state.handleOnLeft == onLeft,
                                onClick = { actions.setHandleSide(onLeft) },
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
                        onValueChange = actions::setHandlePosition,
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
                                onClick = { actions.setHandleSize(handleSize) },
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
                        onCheckedChange = actions::setGestureExclusion,
                        enabled = state.handleEnabled,
                    )
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection("개인정보 보호") {
                    SettingsSwitchRow("앱 잠금", state.lockEnabled, actions::setLock,
                        enabled = state.lockAvailable,
                        detail = if (state.lockAvailable) "지문·얼굴·화면 잠금으로 열어요" else "폰에 화면 잠금을 먼저 설정하세요")
                    SettingsSwitchRow("엣지패널·위젯에서 내용 가리기", state.hidePreviews, actions::setHidePreviews)
                    if (state.hiddenConversations.isNotEmpty()) {
                        Text("숨긴 대화방", style = MaterialTheme.typography.titleSmall)
                        state.hiddenConversations.forEach { conversation ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.title, style = MaterialTheme.typography.bodyLarge)
                                    SettingsHelper(conversation.appLabel)
                                }
                                TextButton(onClick = { actions.unhideConversation(conversation.id) }) { Text("다시 보이기") }
                            }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection("삭제된 메시지") {
                    SettingsSwitchRow("삭제되면 알림 받기", state.deletionAlerts, actions::setDeletionAlerts)
                    if (state.deletionAlerts && !state.notificationPermission) {
                        Surface(color = NotiColors.NoticeBackground, shape = NotiShapes.banner) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("삭제 알림을 받으려면 알림 권한이 필요해요", color = NotiColors.NoticeForeground)
                                TextButton(onClick = actions::requestNotificationPermission) { Text("알림 권한 허용") }
                            }
                        }
                    }
                }
                SettingsSection("사진") {
                    SettingsSwitchRow("사진 미리보기 저장", state.savePhotos, actions::setSavePhotos,
                        detail = "알림에 사진이 함께 오면 미리보기를 폰 안에 저장해요")
                }
                SettingsSection("키워드") {
                    SettingsHelper("이 단어가 들어간 메시지를 강조해요")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.keywords.forEach { word ->
                            InputChip(selected = true, onClick = { actions.removeKeyword(word) },
                                label = { Text(word) }, modifier = Modifier.heightIn(min = 48.dp)
                                    .semantics { contentDescription = "${word} 삭제" },
                                trailingIcon = { Icon(NotiIcons.close, null, Modifier.size(16.dp)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(keyword, { keyword = it }, Modifier.weight(1f),
                            label = { Text("키워드") }, singleLine = true)
                        Button(onClick = { actions.addKeyword(keyword.trim()); keyword = "" },
                            enabled = keyword.isNotBlank() && state.keywords.none { it.equals(keyword.trim(), ignoreCase = true) }) {
                            Text("추가")
                        }
                    }
                }
                SettingsSection("화면") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = state.themeMode == mode, onClick = { actions.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size), icon = {},
                                modifier = Modifier.heightIn(min = 48.dp)) { Text(mode.label) }
                        }
                    }
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
                                            actions.setRetention(days)
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
                SettingsSection("백업") {
                    SettingsHelper("백업 파일은 비밀번호로 암호화돼요. 비밀번호를 잊으면 복원할 수 없어요.")
                    OutlinedButton(onClick = { passwordDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("백업 파일 만들기")
                    }
                    OutlinedButton(onClick = { passwordDialog = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("백업에서 복원")
                    }
                }
                TextButton(onClick = actions::openDiagnostics,
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                    Text("진단 도구 (알림 덤프·제스처 기록)", color = NotiColors.Secondary)
                }
            }
        }
    }

    passwordDialog?.let { backup ->
        BackupPasswordDialog(backup, onDismiss = { passwordDialog = null }, onConfirm = { password ->
            passwordDialog = null
            if (backup) actions.backup(password) else actions.restore(password)
        })
    }

    pendingRetention?.let { days ->
        AlertDialog(
            onDismissRequest = { pendingRetention = null },
            title = { Text("보관 기간을 줄일까요?") },
            text = { Text("${days}일보다 오래된 메시지가 지금 지워져요. 계속할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRetention = null
                    actions.setRetention(days)
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
    val background = NotiColors.Background
    val secondary = NotiColors.Secondary
    val accent = NotiColors.Accent
    Canvas(Modifier.size(56.dp, 104.dp).semantics { contentDescription = description }) {
        val inset = 2.dp.toPx()
        val phoneSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawRoundRect(background, Offset(inset, inset), phoneSize, CornerRadius(10.dp.toPx()))
        drawRoundRect(secondary, Offset(inset, inset), phoneSize,
            CornerRadius(10.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        if (enabled) {
            val barHeight = phoneSize.height * handleSize.heightDp / 800f
            val barWidth = 3.dp.toPx()
            val x = if (onLeft) inset else size.width - inset - barWidth
            val y = inset + (phoneSize.height - barHeight) * position
            drawRoundRect(accent, Offset(x, y), Size(barWidth, barHeight), CornerRadius(barWidth))
        }
    }
}

@Composable
private fun BackupPasswordDialog(backup: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // Passwords stay in transient composition state, never saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val error = when {
        backup && password.length < 6 -> "비밀번호는 6자 이상 입력하세요"
        backup && password != confirmation -> "비밀번호가 일치하지 않아요"
        !backup && password.isEmpty() -> "비밀번호를 입력하세요"
        else -> null
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (backup) "백업 파일 만들기" else "백업에서 복원") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = submitted && error != null)
                if (backup) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("비밀번호 확인") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = submitted && password != confirmation)
                if (submitted && error != null) Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (error == null) onConfirm(password)
            }) { Text(if (backup) "백업" else "복원") }
        })
}
