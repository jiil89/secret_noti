package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jiil.secretnoti.ui.components.MuteChannelsDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.jiil.secretnoti.ui.ContactSheet
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import com.jiil.secretnoti.ui.components.ConversationRow
import com.jiil.secretnoti.ui.components.GlowColorPicker
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: SettingsUi, actions: SettingsActions) {
    var expandedApps by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var passwordDialog by remember { mutableStateOf<Boolean?>(null) }
    var pendingRetention by remember(state.retentionDays) { mutableStateOf<Int?>(null) }
    var showChannelMuteDialog by rememberSaveable { mutableStateOf(false) }
    val positionDescription = stringResource(R.string.settings_screen_position_description)
    val position = state.handlePosition.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
    var showContactSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // 구역마다 스크롤 안에서의 위쪽 위치. 위 칩을 누르면 그 위치로 스크롤하고, 스크롤하면 지나는 구역의 칩이 켜진다.
    val sectionTops = remember { mutableStateMapOf<Int, Int>() }
    fun anchor(index: Int) = Modifier.onGloballyPositioned { sectionTops[index] = it.positionInParent().y.toInt() }
    val sections = listOf(R.string.settings_section_saving, R.string.settings_section_panel, R.string.settings_new_messages,
        R.string.settings_screen_appearance, R.string.settings_screen_privacy, R.string.settings_screen_backup)
    val currentSection by remember {
        derivedStateOf {
            if (scrollState.maxValue in 1..scrollState.value) sections.lastIndex
            else sectionTops.entries.filter { it.value <= scrollState.value + 1 }.maxByOrNull { it.value }?.key ?: 0
        }
    }

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
            SectionChips(sections.map { stringResource(it) }, currentSection) { index ->
                scope.launch { scrollState.animateScrollTo(sectionTops[index] ?: 0) }
            }
            HorizontalDivider(color = NotiColors.Divider)
            Column(
                Modifier.weight(1f).verticalScroll(scrollState).padding(NotiSpacing.content),
                verticalArrangement = Arrangement.spacedBy(NotiSpacing.panel),
            ) {
                // 권한이 모두 있으면 아무것도 보이지 않는다. 빠진 게 있을 때만 맨 위에 알린다.
                if (!state.permissions.allGranted) SettingsPermissions(state.permissions, actions::openPermissions)
                SettingsSection(stringResource(R.string.settings_section_saving), anchor(0)) {
                    SettingsHelper(stringResource(R.string.settings_saving_hint))
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
                    SettingsSwitchRow(stringResource(R.string.settings_auto_mute_ads), state.autoMuteAds,
                        actions::setAutoMuteAds, detail = stringResource(R.string.settings_auto_mute_ads_hint))
                    SettingsSwitchRow(stringResource(R.string.settings_channels_toggle), state.autoMuteChannels,
                        actions::setAutoMuteChannels, detail = stringResource(R.string.settings_channels_hint))
                    if (state.channelConversations.isNotEmpty()) {
                        OutlinedButton(onClick = { showChannelMuteDialog = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button),
                            shape = NotiShapes.banner,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(stringResource(R.string.settings_channels_mute_all, state.channelConversations.size),
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (state.mutedConversations.isNotEmpty()) {
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
                                    Text(conversation.title, style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    SettingsSwitchRow(stringResource(R.string.settings_screen_save_photos), state.savePhotos, actions::setSavePhotos,
                        detail = stringResource(R.string.settings_screen_save_photos_hint))
                    SettingsLabel(stringResource(R.string.settings_screen_retention))
                    SettingsHelper(stringResource(R.string.settings_retention_hint))
                    RetentionSelector(state.retentionDays) { days ->
                        if (requiresRetentionConfirmation(state.retentionDays, days)) pendingRetention = days
                        else actions.setRetention(days)
                    }
                }
                if (showChannelMuteDialog) {
                    MuteChannelsDialog(
                        rooms = state.channelConversations.map { it.title },
                        onConfirm = { delete ->
                            showChannelMuteDialog = false
                            actions.muteAllChannels(delete)
                        },
                        onDismiss = { showChannelMuteDialog = false },
                    )
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_section_panel), anchor(1)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_show_handle), checked = state.handleEnabled,
                        onCheckedChange = actions::setHandleEnabled,
                        detail = stringResource(R.string.settings_screen_show_handle_hint))
                    SettingsLabel(stringResource(R.string.settings_screen_side))
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
                    // 세로 위치는 옆의 작은 폰 그림에서 바로 보인다.
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NotiSpacing.content)) {
                        HandlePreview(state.handleOnLeft, position, state.handleSize, state.handleEnabled)
                        Column(Modifier.weight(1f)) {
                            SettingsLabel(stringResource(R.string.settings_screen_position))
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
                        }
                    }
                    SettingsLabel(stringResource(R.string.settings_screen_size))
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
                    SettingsLabel(stringResource(R.string.settings_panel_width))
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
                    SettingsHelper(if (panelWidthPreset == null) stringResource(R.string.settings_panel_width_custom, state.panelWidthDp)
                        else stringResource(R.string.settings_panel_width_hint))
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_screen_gesture),
                        checked = state.gestureExclusion,
                        onCheckedChange = actions::setGestureExclusion,
                        enabled = state.handleEnabled,
                        detail = stringResource(R.string.settings_screen_gesture_hint),
                    )
                    PanelBackgroundSettings(state, actions)
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_new_messages), anchor(2)) {
                    SettingsSwitchRow(stringResource(R.string.settings_indicator_badge), state.indicatorBadge, actions::setIndicatorBadge,
                        detail = stringResource(R.string.settings_indicator_badge_hint))
                    if (state.indicatorBadge) {
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
                    }
                    SettingsSwitchRow(stringResource(R.string.settings_indicator_glow), state.indicatorGlow, actions::setIndicatorGlow,
                        detail = stringResource(R.string.settings_indicator_glow_hint))
                    if (state.indicatorGlow) {
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
                    }
                    SettingsSwitchRow(stringResource(R.string.settings_peek_bubble), state.peekBubble,
                        actions::setPeekBubble, detail = stringResource(R.string.settings_peek_bubble_hint))
                    SettingsSwitchRow(stringResource(R.string.settings_read_elsewhere_switch),
                        state.readElsewhereAction == ReadElsewhereAction.REMOVE_FROM_PANEL,
                        onCheckedChange = { actions.setReadElsewhereAction(
                            if (it) ReadElsewhereAction.REMOVE_FROM_PANEL else ReadElsewhereAction.KEEP) },
                        detail = stringResource(R.string.settings_read_elsewhere_switch_hint))
                    SettingsLabel(stringResource(R.string.settings_keywords_highlight))
                    SettingsHelper(stringResource(R.string.settings_keywords_hint))
                    if (state.keywords.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.keywords.forEach { word ->
                            val removeDescription = stringResource(R.string.settings_screen_remove_keyword, word)
                            InputChip(selected = true, onClick = { actions.removeKeyword(word) },
                                label = { Text(word) }, modifier = Modifier.heightIn(min = 48.dp)
                                    .semantics { contentDescription = removeDescription },
                                trailingIcon = { Icon(NotiIcons.close, null, Modifier.size(16.dp)) })
                        }
                    }
                    val canAdd = keyword.isNotBlank() && state.keywords.none { it.equals(keyword.trim(), ignoreCase = true) }
                    val addKeyword = { if (canAdd) { actions.addKeyword(keyword.trim()); keyword = "" } }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(keyword, { keyword = it }, Modifier.weight(1f),
                            label = { Text(stringResource(R.string.settings_screen_keywords)) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addKeyword() }))
                        Button(onClick = addKeyword, enabled = canAdd, shape = NotiShapes.banner,
                            modifier = Modifier.height(56.dp)) {
                            Text(stringResource(R.string.settings_screen_add))
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_appearance), anchor(3)) {
                    SettingsLabel(stringResource(R.string.settings_theme))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = state.themeMode == mode, onClick = { actions.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size), icon = {},
                                modifier = Modifier.heightIn(min = NotiSpacing.button)) { Text(stringResource(mode.labelRes)) }
                        }
                    }
                    SettingsLabel(stringResource(R.string.settings_text_size))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        TextSize.entries.forEachIndexed { index, size ->
                            SegmentedButton(selected = state.textSize == size, onClick = { actions.setTextSize(size) },
                                shape = SegmentedButtonDefaults.itemShape(index, TextSize.entries.size), icon = {},
                                modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                                Text(stringResource(size.labelRes), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    SettingsLabel(stringResource(R.string.settings_preview_lines))
                    SettingsHelper(stringResource(R.string.settings_preview_lines_hint))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        PreviewLines.entries.forEachIndexed { index, lines ->
                            SegmentedButton(selected = state.previewLines == lines, onClick = { actions.setPreviewLines(lines) },
                                shape = SegmentedButtonDefaults.itemShape(index, PreviewLines.entries.size), icon = {},
                                modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                                Text(stringResource(lines.labelRes), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                    TextSizePreview(state.previewLines)
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_privacy), anchor(4)) {
                    SettingsSwitchRow(stringResource(R.string.settings_screen_lock), state.lockEnabled, actions::setLock,
                        enabled = state.lockAvailable,
                        detail = stringResource(if (state.lockAvailable) R.string.settings_screen_lock_hint
                            else R.string.settings_screen_lock_unavailable))
                    SettingsSwitchRow(stringResource(R.string.settings_screen_hide_previews), state.hidePreviews, actions::setHidePreviews,
                        detail = stringResource(R.string.settings_screen_hide_previews_hint))
                    SettingsSwitchRow(stringResource(R.string.settings_screen_deletion_alerts), state.deletionAlerts, actions::setDeletionAlerts,
                        detail = stringResource(R.string.settings_screen_deletion_alerts_hint))
                    if (state.deletionAlerts && !state.notificationPermission) {
                        Surface(color = NotiColors.NoticeBackground, shape = NotiShapes.banner) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(stringResource(R.string.settings_screen_notification_required), color = NotiColors.NoticeForeground)
                                TextButton(onClick = actions::requestNotificationPermission) { Text(stringResource(R.string.settings_screen_allow_notifications)) }
                            }
                        }
                    }
                    if (state.hiddenConversations.isNotEmpty()) {
                        SettingsLabel(stringResource(R.string.settings_screen_hidden))
                        state.hiddenConversations.forEach { conversation ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppIcon(conversation.packageName, 24.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.title, style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    SettingsHelper(conversation.appLabel)
                                }
                                TextButton(onClick = { actions.unhideConversation(conversation.id) },
                                    modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                                    Text(stringResource(R.string.settings_screen_unhide))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_screen_backup), anchor(5)) {
                    SettingsHelper(stringResource(R.string.settings_screen_backup_hint))
                    Row(horizontalArrangement = Arrangement.spacedBy(NotiSpacing.small)) {
                        OutlinedButton(onClick = { passwordDialog = true }, shape = NotiShapes.banner,
                            modifier = Modifier.weight(1f).heightIn(min = NotiSpacing.button)) {
                            Text(stringResource(R.string.settings_screen_create_backup), textAlign = TextAlign.Center)
                        }
                        OutlinedButton(onClick = { passwordDialog = false }, shape = NotiShapes.banner,
                            modifier = Modifier.weight(1f).heightIn(min = NotiSpacing.button)) {
                            Text(stringResource(R.string.settings_screen_restore_backup), textAlign = TextAlign.Center)
                        }
                    }
                }
                HorizontalDivider(color = NotiColors.Divider)
                SettingsSection(stringResource(R.string.settings_section_help)) {
                    SettingsNavRow(stringResource(R.string.settings_help), stringResource(R.string.settings_help_hint), actions::openHelp)
                    SettingsNavRow(stringResource(R.string.settings_contact), stringResource(R.string.settings_contact_hint)) {
                        showContactSheet = true
                    }
                }
                // 맨 아래: 진단 기록 · 버전 (테스터가 어떤 판을 쓰는지 바로 알 수 있게).
                val context = LocalContext.current
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = actions::openDiagnostics, modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                        Text(stringResource(R.string.settings_screen_diagnostics), color = NotiColors.Secondary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (version != null) Text(stringResource(R.string.settings_screen_version, version),
                        color = NotiColors.Secondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showContactSheet) ContactSheet(
        onPick = { showContactSheet = false; actions.openContact(it) },
        onDismiss = { showContactSheet = false },
    )

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

/** 패널 투명도 슬라이더(10% 단위)와 뒤 화면 흐림 스위치. 바꾸면 다음에 패널을 열 때 적용된다. */
@Composable
private fun PanelBackgroundSettings(state: SettingsUi, actions: SettingsActions) {
    val description = stringResource(R.string.settings_panel_transparency)
    Column(verticalArrangement = Arrangement.spacedBy(NotiSpacing.tiny)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SettingsLabel(description) }
            Text(stringResource(R.string.settings_panel_transparency_value, state.panelTransparency),
                style = MaterialTheme.typography.bodyMedium, color = NotiColors.Text)
        }
        Slider(
            value = state.panelTransparency.toFloat(),
            onValueChange = { v ->
                val step = ((v / 10f).roundToInt() * 10).coerceIn(0, PANEL_TRANSPARENCY_MAX)
                if (step != state.panelTransparency) actions.setPanelTransparency(step)
            },
            valueRange = 0f..PANEL_TRANSPARENCY_MAX.toFloat(),
            steps = PANEL_TRANSPARENCY_MAX / 10 - 1,
            modifier = Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button)
                .semantics { contentDescription = description },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SettingsHelper(stringResource(R.string.settings_panel_transparency_opaque))
            SettingsHelper(stringResource(R.string.settings_panel_transparency_clear))
        }
        SettingsHelper(stringResource(R.string.settings_panel_transparency_hint))
    }
    SettingsSwitchRow(stringResource(R.string.settings_panel_blur), state.panelBlur && state.panelBlurSupported,
        actions::setPanelBlur, enabled = state.panelBlurSupported,
        detail = stringResource(when {
            !state.panelBlurSupported -> R.string.settings_panel_blur_unsupported
            state.panelBlur && !state.panelBlurEnabledNow -> R.string.settings_panel_blur_off_now
            else -> R.string.settings_panel_blur_hint
        }))
}

/** 글자 크기·줄 수를 고르면 바로 바뀌는 대화방 한 줄 미리보기 (설정 화면도 같은 배율이 적용된다). */
@Composable
private fun TextSizePreview(previewLines: PreviewLines) {
    val sample = ConversationUi(
        id = -1, source = com.jiil.secretnoti.capture.Source.KAKAO, packageName = "com.kakao.talk",
        appLabel = stringResource(R.string.source_kakao), title = stringResource(R.string.settings_text_preview_name),
        isGroup = false, lastSender = null, preview = stringResource(R.string.settings_text_preview_message),
        lastAt = System.currentTimeMillis() - 3 * 60_000, unread = 2,
    )
    Surface(shape = NotiShapes.banner, color = NotiColors.DetailBackground) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            SettingsHelper(stringResource(R.string.settings_text_preview))
            Surface(shape = NotiShapes.banner, color = NotiColors.Background, modifier = Modifier.padding(top = 6.dp)) {
                ConversationRow(sample, onClick = {}, modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = previewLines.lines.takeIf { it > 0 } ?: 2)
            }
        }
    }
}

/**
 * 보관 기간 6개를 한 줄에. 세그먼트 버튼은 안쪽 여백이 커서 "180일"이 잘리므로 여백 없이 직접 그린다.
 */
@Composable
private fun RetentionSelector(selectedDays: Int, onSelect: (Int) -> Unit) {
    val outline = NotiColors.Outline
    Row(Modifier.fillMaxWidth().height(NotiSpacing.button).clip(NotiShapes.pill)
        .border(1.dp, outline, NotiShapes.pill).selectableGroup()) {
        RETENTION_OPTIONS.forEachIndexed { index, days ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
            val selected = selectedDays == days
            Box(Modifier.weight(1f).fillMaxHeight()
                .background(if (selected) NotiColors.NoticeBackground else NotiColors.Background)
                .selectable(selected, role = Role.RadioButton, onClick = { if (!selected) onSelect(days) }),
                contentAlignment = Alignment.Center) {
                Text(when (days) {
                    0 -> stringResource(R.string.settings_retention_forever_short)
                    365 -> stringResource(R.string.settings_screen_year)
                    else -> pluralStringResource(R.plurals.settings_screen_days, days, days)
                }, maxLines = 1, softWrap = false,
                    color = if (selected) NotiColors.NoticeForeground else NotiColors.Body,
                    fontWeight = if (selected) FontWeight.Bold else null,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeightStyle = SingleLineCentered))
            }
        }
    }
}

private fun requiresRetentionConfirmation(current: Int, next: Int): Boolean =
    next != 0 && (current == 0 || next < current)

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NotiSpacing.medium)) {
        Text(title, modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium, color = NotiColors.Text)
        content()
    }
}

/** 설정 맨 위에 붙어 있는 구역 칩. 누르면 그 구역으로 스크롤한다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = NotiSpacing.content)
        .selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            // 좁은 화면에서 칩이 넘치면, 켜진 칩이 보이도록 칩 줄도 따라 움직인다.
            val requester = remember { BringIntoViewRequester() }
            LaunchedEffect(on) { if (on) requester.bringIntoView() }
            Box(Modifier.bringIntoViewRequester(requester).heightIn(min = 48.dp)
                .selectable(on, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center) {
                Surface(shape = NotiShapes.pill, color = if (on) NotiColors.Text else NotiColors.Background,
                    border = if (on) null else BorderStroke(1.dp, NotiColors.Outline)) {
                    Box(Modifier.heightIn(min = 34.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (on) NotiColors.Background else NotiColors.Body, maxLines = 1,
                            style = MaterialTheme.typography.labelLarge.copy(lineHeightStyle = SingleLineCentered))
                    }
                }
            }
        }
    }
}

/** 다른 화면으로 넘어가는 한 줄 (도움말·문의하기). */
@Composable
private fun SettingsNavRow(label: String, detail: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button).clickable(role = Role.Button, onClick = onClick)
        .padding(vertical = NotiSpacing.small),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NotiSpacing.medium)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NotiSpacing.tiny)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = NotiColors.Text)
            if (detail != null) SettingsHelper(detail)
        }
        Icon(NotiIcons.chevronLeft, null, Modifier.size(20.dp).graphicsLayer { scaleX = -1f }, tint = NotiColors.Secondary)
    }
}

/** 구역 안의 작은 제목 (세그먼트 버튼·슬라이더 위). */
@Composable
private fun SettingsLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = NotiColors.Text)
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

/** 빠진 권한이 있을 때만 맨 위에 보이는 경고 카드. */
@Composable
private fun SettingsPermissions(permissions: PermissionSummaryUi, onOpenPermissions: () -> Unit) {
    val missing = buildList {
        if (!permissions.notificationAccess) add(stringResource(R.string.settings_screen_notification_access))
        if (!permissions.overlay) add(stringResource(R.string.settings_screen_overlay))
        if (!permissions.batteryExempt) add(stringResource(R.string.settings_screen_battery))
    }
    Surface(shape = NotiShapes.banner, color = NotiColors.NoticeBackground) {
        Column(Modifier.fillMaxWidth().padding(NotiSpacing.content),
            verticalArrangement = Arrangement.spacedBy(NotiSpacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(NotiSpacing.small),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(NotiIcons.info, null, Modifier.size(20.dp), tint = NotiColors.NoticeForeground)
                Text(stringResource(R.string.settings_screen_permissions_needed),
                    style = MaterialTheme.typography.titleSmall, color = NotiColors.NoticeForeground)
            }
            Text(stringResource(R.string.settings_screen_missing_permissions, missing.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium, color = NotiColors.NoticeForeground)
            TextButton(onClick = onOpenPermissions, modifier = Modifier.heightIn(min = NotiSpacing.button)) {
                Text(stringResource(R.string.settings_screen_permissions_settings), color = NotiColors.NoticeForeground)
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
