package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.jiil.secretnoti.ui.components.GlowColorPicker
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: SettingsUi, actions: SettingsActions) {
    var expandedApps by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var passwordDialog by remember { mutableStateOf<Boolean?>(null) }
    var pendingRetention by remember(state.retentionDays) { mutableStateOf<Int?>(null) }
    var showChannelMuteDialog by rememberSaveable { mutableStateOf(false) }
    var deleteChannelHistory by rememberSaveable { mutableStateOf(false) }
    val positionDescription = stringResource(R.string.settings_screen_position_description)
    val position = state.handlePosition.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f

    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(NotiSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions::back, modifier = Modifier.size(NotiSpacing.button)) {
                    Icon(NotiIcons.chevronLeft, contentDescription = stringResource(R.string.settings_screen_back), tint = NotiColors.Body)
                }
                Text(stringResource(R.string.settings_screen_title), style = MaterialTheme.typography.titleLarge, color = NotiColors.Text)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(NotiSpacing.content),
                verticalArrangement = Arrangement.spacedBy(NotiSpacing.panel),
            ) {
                SettingsPermissions(state.permissions, actions::openPermissions)
                SettingsSection(stringResource(R.string.settings_screen_apps)) {
                    val suggested = state.apps.filter { it.suggested }
                    val others = state.apps.filterNot { it.suggested }
                    (suggested + if (expandedApps) others else emptyList()).forEach { app ->
                        SettingsSwitchRow(app.label, app.enabled,
                            onCheckedChange = { actions.toggleApp(app.packageName, it) }, detail = app.detail,
                            leading = { AppIcon(app.packageName, 24.dp) })
                    }
                    if (others.isNotEmpty()) TextButton(onClick = { expandedApps = !expandedApps }) {
                        Text(if (expandedApps) stringResource(R.string.settings_screen_collapse_apps) else pluralStringResource(R.plurals.settings_screen_more_apps, others.size, others.size))
                    }
                    SettingsHelper(stringResource(R.string.settings_screen_apps_hint))
                    SettingsSwitchRow(stringResource(R.string.settings_auto_mute_ads), state.autoMuteAds,
                        actions::setAutoMuteAds, detail = stringResource(R.string.settings_auto_mute_ads_hint))
                }
                if (state.mutedConversations.isNotEmpty()) {
                    HorizontalDivider(color = NotiColors.Divider)
                    SettingsSection(stringResource(R.string.settings_muted_conversations, state.mutedConversations.size)) {
                        SettingsHelper(stringResource(R.string.settings_muted_conversations_hint))
                        // 많아지면 길어지므로 평소에는 접어 두고 "보기"로 펼친다 (설정을 새로 열면 다시 접힘).
                        var mutedExpanded by rememberSaveable { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().clip(NotiShapes.banner).background(NotiColors.DetailBackground)
                            .clickable(role = Role.Button) { mutedExpanded = !mutedExpanded }
                            .heightIn(min = 48.dp).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.conversations_muted_collapsed, state.mutedConversations.size),
                                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = NotiColors.Body)
                            Text(stringResource(if (mutedExpanded) R.string.conversations_muted_hide else R.string.conversations_muted_show),
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                                color = NotiColors.NoticeForeground)
                        }
                        if (mutedExpanded) state.mutedConversations.forEach { conversation ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppIcon(conversation.packageName, 24.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.title, style = MaterialTheme.typography.bodyLarge)
                                    SettingsHelper(stringResource(if (conversation.hasRecords) R.string.settings_muted_records_kept
                                        else R.string.settings_muted_records_deleted, conversation.appLabel))
                                }
                                TextButton(onClick = { actions.unmuteConversation(conversation.id) },
                                    modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                                    Text(stringResource(R.string.settings_unmute_conversation))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_channels_section)) {
                    SettingsSwitchRow(stringResource(R.string.settings_channels_toggle), state.autoMuteChannels,
                        actions::setAutoMuteChannels, detail = stringResource(R.string.settings_channels_hint))
                    if (state.channelConversations.isEmpty()) {
                        SettingsHelper(stringResource(R.string.settings_channels_empty))
                    } else {
                        OutlinedButton(onClick = { deleteChannelHistory = false; showChannelMuteDialog = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                            Text(stringResource(R.string.settings_channels_mute_all, state.channelConversations.size),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                if (showChannelMuteDialog) {
                    val rooms = state.channelConversations
                    AlertDialog(
                        onDismissRequest = { showChannelMuteDialog = false },
                        title = { Text(stringResource(R.string.settings_channels_confirm_title, rooms.size)) },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(NotiSpacing.medium)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rooms.take(6).forEach { room ->
                                        Text(room.title, Modifier.widthIn(max = 120.dp)
                                            .background(NotiColors.DetailBackground, NotiShapes.pill)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                    if (rooms.size > 6) Text(stringResource(R.string.settings_channels_more, rooms.size - 6),
                                        Modifier.background(NotiColors.DetailBackground, NotiShapes.pill)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium)
                                }
                                Column(Modifier.selectableGroup()) {
                                    listOf(true, false).forEach { delete ->
                                        Row(Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
                                            .selectable(deleteChannelHistory == delete, role = Role.RadioButton,
                                                onClick = { deleteChannelHistory = delete }),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = deleteChannelHistory == delete, onClick = null)
                                            Text(stringResource(if (delete) R.string.conversation_mute_delete_existing
                                                else R.string.conversation_mute_keep_existing), Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        },
                        dismissButton = { TextButton(onClick = { showChannelMuteDialog = false }) {
                            Text(stringResource(R.string.conversations_cancel))
                        } },
                        confirmButton = { TextButton(onClick = {
                            showChannelMuteDialog = false
                            actions.muteAllChannels(deleteChannelHistory)
                        }) { Text(stringResource(R.string.settings_channels_confirm_action)) } },
                    )
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_read_elsewhere)) {
                    SettingsHelper(stringResource(R.string.settings_read_elsewhere_hint))
                    Column(Modifier.selectableGroup()) {
                        ReadElsewhereAction.entries.forEach { action ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(state.readElsewhereAction == action, role = Role.RadioButton,
                                    onClick = { actions.setReadElsewhereAction(action) })
                                .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RadioButton(selected = state.readElsewhereAction == action, onClick = null)
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(action.labelRes), style = MaterialTheme.typography.bodyLarge)
                                    if (action == ReadElsewhereAction.REMOVE_FROM_PANEL) {
                                        SettingsHelper(stringResource(R.string.settings_read_elsewhere_remove_hint))
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_handle)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_show_handle), checked = state.handleEnabled,
                        onCheckedChange = actions::setHandleEnabled)
                    Text(stringResource(R.string.settings_screen_side), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(true, false).forEachIndexed { index, onLeft ->
                            SegmentedButton(
                                selected = state.handleOnLeft == onLeft,
                                onClick = { actions.setHandleSide(onLeft) },
                                enabled = state.handleEnabled,
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                                icon = {},
                            ) { Text(if (onLeft) stringResource(R.string.settings_screen_left) else stringResource(R.string.settings_screen_right)) }
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
                                Text(stringResource(R.string.settings_screen_handle_preview), style = MaterialTheme.typography.labelLarge)
                                SettingsHelper(stringResource(R.string.settings_screen_handle_hint))
                            }
                        }
                    }
                    Text(stringResource(R.string.settings_screen_position), style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = position,
                        onValueChange = actions::setHandlePosition,
                        valueRange = 0f..1f,
                        enabled = state.handleEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
                            .semantics { contentDescription = positionDescription },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SettingsHelper(stringResource(R.string.settings_screen_top))
                        SettingsHelper(stringResource(R.string.settings_screen_bottom))
                    }
                    Text(stringResource(R.string.settings_screen_size), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        HandleSize.entries.forEachIndexed { index, handleSize ->
                            SegmentedButton(
                                selected = state.handleSize == handleSize,
                                onClick = { actions.setHandleSize(handleSize) },
                                enabled = state.handleEnabled,
                                shape = SegmentedButtonDefaults.itemShape(index, HandleSize.entries.size),
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                                icon = {},
                            ) { Text(stringResource(handleSize.labelRes)) }
                        }
                    }
                    Text(stringResource(R.string.settings_panel_width), style = MaterialTheme.typography.titleSmall)
                    val panelWidthPreset = PanelWidth.of(state.panelWidthDp)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        PanelWidth.entries.forEachIndexed { index, preset ->
                            SegmentedButton(
                                selected = panelWidthPreset == preset,
                                onClick = { actions.setPanelWidth(preset.dp) },
                                shape = SegmentedButtonDefaults.itemShape(index, PanelWidth.entries.size),
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                                icon = {},
                            ) { Text(stringResource(preset.labelRes)) }
                        }
                    }
                    if (panelWidthPreset == null) {
                        SettingsHelper(stringResource(R.string.settings_panel_width_custom, state.panelWidthDp))
                    }
                    SettingsHelper(stringResource(R.string.settings_panel_width_hint))
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_screen_gesture),
                        detail = stringResource(R.string.settings_screen_gesture_hint),
                        checked = state.gestureExclusion,
                        onCheckedChange = actions::setGestureExclusion,
                        enabled = state.handleEnabled,
                    )
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_new_messages)) {
                    SettingsHelper(stringResource(R.string.settings_new_messages_hint))
                    SettingsSwitchRow(stringResource(R.string.settings_indicator_badge), state.indicatorBadge,
                        actions::setIndicatorBadge, detail = stringResource(R.string.settings_indicator_badge_hint))
                    if (state.indicatorBadge) {
                        Text(stringResource(R.string.settings_badge_style), style = MaterialTheme.typography.titleSmall)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            BadgeStyle.entries.forEachIndexed { index, style ->
                                SegmentedButton(
                                    selected = state.badgeStyle == style,
                                    onClick = { actions.setBadgeStyle(style) },
                                    shape = SegmentedButtonDefaults.itemShape(index, BadgeStyle.entries.size),
                                    modifier = Modifier.heightIn(min = NotiSpacing.button), icon = {},
                                ) { Text(stringResource(style.labelRes)) }
                            }
                        }
                        if (state.badgeStyle == BadgeStyle.DOT) SettingsHelper(stringResource(R.string.settings_badge_style_dot_hint))
                    }
                    SettingsSwitchRow(stringResource(R.string.settings_indicator_glow), state.indicatorGlow,
                        actions::setIndicatorGlow, detail = stringResource(R.string.settings_indicator_glow_hint))
                    if (state.indicatorGlow) {
                        Text(stringResource(R.string.settings_glow_color), style = MaterialTheme.typography.titleSmall)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf(true, false).forEachIndexed { index, useAppColor ->
                                SegmentedButton(
                                    selected = state.glowUsesAppColor == useAppColor,
                                    onClick = { actions.setGlowUsesAppColor(useAppColor) },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                                    modifier = Modifier.heightIn(min = NotiSpacing.button), icon = {},
                                ) {
                                    Text(stringResource(if (useAppColor) R.string.settings_glow_app_color else R.string.settings_glow_custom_color))
                                }
                            }
                        }
                        if (!state.glowUsesAppColor) GlowColorPicker(state.glowColor, actions::setGlowColor)
                        TextButton(onClick = actions::previewGlow) { Text(stringResource(R.string.settings_glow_preview)) }
                        SettingsHelper(stringResource(R.string.settings_glow_preview_hint))
                    }
                    SettingsSwitchRow(stringResource(R.string.settings_peek_bubble), state.peekBubble,
                        actions::setPeekBubble, detail = stringResource(R.string.settings_peek_bubble_hint))
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_privacy)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_lock), state.lockEnabled, actions::setLock,
                        enabled = state.lockAvailable,
                        detail = if (state.lockAvailable) stringResource(R.string.settings_screen_lock_hint) else stringResource(R.string.settings_screen_lock_unavailable))
                    SettingsSwitchRow(stringResource(R.string.settings_screen_hide_previews), state.hidePreviews, actions::setHidePreviews)
                    if (state.hiddenConversations.isNotEmpty()) {
                        Text(stringResource(R.string.settings_screen_hidden), style = MaterialTheme.typography.titleSmall)
                        state.hiddenConversations.forEach { conversation ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.title, style = MaterialTheme.typography.bodyLarge)
                                    SettingsHelper(conversation.appLabel)
                                }
                                TextButton(onClick = { actions.unhideConversation(conversation.id) }) { Text(stringResource(R.string.settings_screen_unhide)) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_deleted_messages)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_deletion_alerts), state.deletionAlerts, actions::setDeletionAlerts)
                    if (state.deletionAlerts && !state.notificationPermission) {
                        Surface(color = NotiColors.NoticeBackground, shape = NotiShapes.banner) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(stringResource(R.string.settings_screen_notification_required), color = NotiColors.NoticeForeground)
                                TextButton(onClick = actions::requestNotificationPermission) { Text(stringResource(R.string.settings_screen_allow_notifications)) }
                            }
                        }
                    }
                }
                SettingsSection(stringResource(R.string.settings_screen_photos)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_save_photos), state.savePhotos, actions::setSavePhotos,
                        detail = stringResource(R.string.settings_screen_photos_hint))
                }
                SettingsSection(stringResource(R.string.settings_screen_keywords)) {
                    SettingsHelper(stringResource(R.string.settings_screen_keywords_hint))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.keywords.forEach { word ->
                            val removeDescription = stringResource(R.string.settings_screen_remove_keyword, word)
                            InputChip(selected = true, onClick = { actions.removeKeyword(word) },
                                label = { Text(word) }, modifier = Modifier.heightIn(min = 48.dp)
                                    .semantics { contentDescription = removeDescription },
                                trailingIcon = { Icon(NotiIcons.close, null, Modifier.size(16.dp)) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(keyword, { keyword = it }, Modifier.weight(1f),
                            label = { Text(stringResource(R.string.settings_screen_keywords)) }, singleLine = true)
                        Button(onClick = { actions.addKeyword(keyword.trim()); keyword = "" },
                            enabled = keyword.isNotBlank() && state.keywords.none { it.equals(keyword.trim(), ignoreCase = true) }) {
                            Text(stringResource(R.string.settings_screen_add))
                        }
                    }
                }
                SettingsSection(stringResource(R.string.settings_screen_appearance)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = state.themeMode == mode, onClick = { actions.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size), icon = {},
                                modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(mode.labelRes)) }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_retention)) {
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
                                label = { Text(when (days) { 0 -> stringResource(R.string.settings_screen_forever); 365 -> stringResource(R.string.settings_screen_year); else -> pluralStringResource(R.plurals.settings_screen_days, days, days) }) },
                                modifier = Modifier.heightIn(min = NotiSpacing.button),
                            )
                        }
                    }
                    SettingsHelper(stringResource(R.string.settings_screen_retention_hint))
                }
                SettingsSection(stringResource(R.string.settings_screen_backup)) {
                    SettingsHelper(stringResource(R.string.settings_screen_backup_hint))
                    OutlinedButton(onClick = { passwordDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.settings_screen_create_backup))
                    }
                    OutlinedButton(onClick = { passwordDialog = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.settings_screen_restore_backup))
                    }
                }
                TextButton(onClick = actions::openDiagnostics,
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)) {
                    Text(stringResource(R.string.settings_screen_diagnostics), color = NotiColors.Secondary)
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
            title = { Text(stringResource(R.string.settings_screen_shorten_retention)) },
            text = { Text(pluralStringResource(R.plurals.settings_screen_retention_warning, days, days)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRetention = null
                    actions.setRetention(days)
                }, modifier = Modifier.heightIn(min = NotiSpacing.button)) { Text(stringResource(R.string.settings_screen_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRetention = null },
                    modifier = Modifier.heightIn(min = NotiSpacing.button)) { Text(stringResource(R.string.settings_screen_cancel)) }
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
        if (!permissions.notificationAccess) add(stringResource(R.string.settings_screen_notification_access))
        if (!permissions.overlay) add(stringResource(R.string.settings_screen_overlay))
        if (!permissions.batteryExempt) add(stringResource(R.string.settings_screen_battery))
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
                Text(if (permissions.allGranted) stringResource(R.string.settings_screen_permissions_granted) else stringResource(R.string.settings_screen_permissions_needed),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (permissions.allGranted) NotiColors.Body else NotiColors.NoticeForeground)
            }
            if (!permissions.allGranted) {
                Text(stringResource(R.string.settings_screen_missing_permissions, missing.joinToString(" / ")),
                    style = MaterialTheme.typography.bodyMedium, color = NotiColors.NoticeForeground)
                TextButton(onClick = onOpenPermissions, modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                    Text(stringResource(R.string.settings_screen_permissions_settings), color = NotiColors.NoticeForeground)
                }
            }
        }
    }
}

@Composable
private fun HandlePreview(onLeft: Boolean, position: Float, handleSize: HandleSize, enabled: Boolean) {
    val description = if (enabled) {
        stringResource(R.string.settings_screen_handle_description,
            stringResource(if (onLeft) R.string.settings_screen_left else R.string.settings_screen_right),
            (position * 100).toInt(), stringResource(handleSize.labelRes))
    } else stringResource(R.string.settings_screen_handle_hidden)
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
        backup && password.length < 6 -> stringResource(R.string.settings_screen_password_length)
        backup && password != confirmation -> stringResource(R.string.settings_screen_password_mismatch)
        !backup && password.isEmpty() -> stringResource(R.string.settings_screen_password_required)
        else -> null
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (backup) stringResource(R.string.settings_screen_create_backup) else stringResource(R.string.settings_screen_restore_backup)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.settings_screen_password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = submitted && error != null)
                if (backup) OutlinedTextField(confirmation, { confirmation = it }, label = { Text(stringResource(R.string.settings_screen_password_confirmation)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = submitted && password != confirmation)
                if (submitted && error != null) Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_screen_cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (error == null) onConfirm(password)
            }) { Text(if (backup) stringResource(R.string.settings_screen_backup) else stringResource(R.string.settings_screen_restore)) }
        })
}
