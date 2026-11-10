package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.BorderStroke

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.components.*
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

private fun visibleConversations(conversations: List<ConversationUi>, filter: String?) =
    conversations.filter { filter == null || it.packageName == filter }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EdgePanel(
    conversations: List<ConversationUi>,
    availableApps: List<AppRef>,
    filter: String?,
    onFilterChange: (String?) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onShowAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onLeftSide: Boolean,
    width: Dp,
    minWidth: Dp,
    maxWidth: Dp,
    onWidthChange: (Dp) -> Unit,
    onWidthChangeFinished: () -> Unit,
    onDismiss: (Long) -> Unit,
    onDismissAll: () -> Unit,
    onMute: (id: Long, deleteExisting: Boolean) -> Unit,
    shape: Shape = NotiShapes.panel(onLeftSide),
    /** false면 배경색·그림자를 그리지 않는다 (오버레이 창이 배경과 흐림을 그릴 때). */
    drawBackground: Boolean = true,
    /** true면 대화방 수만큼만 높아진다 (최대 [maxHeight]). false면 [maxHeight]까지 채운다. */
    wrapHeight: Boolean = false,
    maxHeight: Dp = 652.dp,
) {
    var pendingMute by remember { mutableStateOf<ConversationUi?>(null) }
    var resizing by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartWidth by remember { mutableStateOf(width) }
    val currentWidth by rememberUpdatedState(width)
    val changeWidth by rememberUpdatedState(onWidthChange)
    val finishWidthChange by rememberUpdatedState(onWidthChangeFinished)
    val resizeDescription = stringResource(R.string.panel_resize_description)
    val widenLabel = stringResource(R.string.settings_panel_width_wide)
    val narrowLabel = stringResource(R.string.settings_panel_width_narrow)
    // 반투명이면 그림자가 패널 안으로 비쳐 탁해 보이므로, 그림자 대신 얇은 테두리를 쓴다.
    val opacity = LocalPanelOpacity.current
    val translucent = opacity < 1f
    // 빼기 확인 카드가 들어갈 만큼은 높이를 확보한다.
    Surface(Modifier.width(width).heightIn(min = if (pendingMute != null) 360.dp else 0.dp, max = maxHeight),
        shape = shape, color = if (drawBackground) NotiColors.Background.copy(alpha = opacity) else Color.Transparent,
        shadowElevation = if (translucent || !drawBackground) 0.dp else 16.dp,
        border = if (translucent) BorderStroke(1.dp, NotiColors.Divider) else null) {
        Box {
            Column((if (wrapHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize()).padding(
                start = if (onLeftSide) 16.dp else 20.dp,
                end = if (onLeftSide) 20.dp else 16.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 머리: 앱 아이콘 + 제목 + 설정. "읽음 표시 안 남음" 안내는 패널에서는 뺐다 (앱 화면에는 남김).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecretNotiMark()
                    Text(stringResource(R.string.conversations_recent), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    SettingsButton(onOpenSettings)
                }
                AppFilterChips(availableApps, filter, onFilterChange)
                ConversationItems(visibleConversations(conversations, filter).take(10),
                    onOpenConversation, Modifier.weight(1f, fill = !wrapHeight), maxLines = if (width >= 340.dp) 3 else 2,
                    onDismiss = onDismiss, onMute = { pendingMute = it })
                Column {
                    HorizontalDivider(color = NotiColors.Divider)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onShowAll, modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(stringResource(R.string.conversations_all_history), color = NotiColors.NoticeForeground)
                        }
                        // 패널에서만 빼는 것이라 기록은 남는다. 뺄 대화방이 없으면 숨긴다.
                        if (conversations.isNotEmpty()) TextButton(onClick = onDismissAll,
                            modifier = Modifier.heightIn(min = NotiSpacing.button),
                            contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(NotiIcons.trash, null, Modifier.size(18.dp), tint = NotiColors.Secondary)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.panel_clear_all), color = NotiColors.Secondary,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Box(
                Modifier.align(if (onLeftSide) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                    .width(24.dp).height(96.dp)
                    .semantics {
                        contentDescription = resizeDescription
                        customActions = listOf(
                            CustomAccessibilityAction(widenLabel) {
                                changeWidth((currentWidth + 20.dp).coerceIn(minWidth, maxWidth))
                                finishWidthChange()
                                true
                            },
                            CustomAccessibilityAction(narrowLabel) {
                                changeWidth((currentWidth - 20.dp).coerceIn(minWidth, maxWidth))
                                finishWidthChange()
                                true
                            },
                        )
                    }
                    // 화면 기준 좌표(rawX)로 계산한다. 폭이 바뀌면 오버레이 창도 같이 움직여서
                    // 창 안 좌표로 재면 손가락 위치가 흔들린다.
                    .pointerInteropFilter { e ->
                        when (e.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                dragStartX = e.rawX
                                dragStartWidth = currentWidth.coerceIn(minWidth, maxWidth)
                                resizing = true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val delta = with(density) { (e.rawX - dragStartX).toDp() } * (if (onLeftSide) 1f else -1f)
                                changeWidth((dragStartWidth + delta).coerceIn(minWidth, maxWidth))
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                resizing = false
                                finishWidthChange()
                            }
                        }
                        true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(4.dp).height(40.dp)
                    .background(if (resizing) NotiColors.Accent else NotiColors.Outline, NotiShapes.pill))
            }
            // 오버레이 창에서는 Dialog를 띄울 수 없으므로 패널 안에 확인 카드를 겹쳐 보여준다.
            pendingMute?.let { conversation ->
                Box(
                    Modifier.matchParentSize().background(NotiColors.Scrim)
                        .pointerInput(Unit) { detectTapGestures { pendingMute = null } },
                    contentAlignment = Alignment.Center,
                ) {
                    MuteConversationCard(
                        conversation.title,
                        onConfirm = { pendingMute = null; onMute(conversation.id, it) },
                        onDismiss = { pendingMute = null },
                        modifier = Modifier.padding(12.dp).pointerInput(Unit) { detectTapGestures { } },
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationListScreen(
    conversations: List<ConversationUi>,
    availableApps: List<AppRef>,
    filter: String?,
    onFilterChange: (String?) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onTogglePin: (Long) -> Unit,
    onHide: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onMute: (id: Long, deleteExisting: Boolean) -> Unit,
    mutedConversations: List<ConversationUi>,
    onUnmute: (Long) -> Unit,
) {
    var pendingMute by remember { mutableStateOf<ConversationUi?>(null) }
    var pendingDelete by remember { mutableStateOf<ConversationUi?>(null) }
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = NotiSpacing.content)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.conversations_all_messages), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderButton(NotiIcons.search, stringResource(R.string.conversations_search), onOpenSearch)
                    SettingsButton(onOpenSettings)
                }
            }
            Spacer(Modifier.height(6.dp))
            AppFilterChips(availableApps, filter, onFilterChange)
            Spacer(Modifier.height(8.dp))
            ConversationItems(visibleConversations(conversations, filter), onOpenConversation, Modifier.weight(1f),
                onTogglePin = onTogglePin, onHide = onHide, onDelete = { pendingDelete = it }, onMute = { pendingMute = it },
                mutedConversations = visibleConversations(mutedConversations, filter), onUnmute = onUnmute)
        }
    }
    pendingMute?.let { conversation ->
        MuteConversationDialog(conversation.title,
            onConfirm = { pendingMute = null; onMute(conversation.id, it) },
            onDismiss = { pendingMute = null })
    }
    pendingDelete?.let { conversation ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.conversations_delete_history)) },
            text = { Text(stringResource(R.string.conversations_delete_confirm)) },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.conversations_cancel)) } },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; onDelete(conversation.id) }) {
                    Text(stringResource(R.string.conversations_delete), color = MaterialTheme.colorScheme.error)
                }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationItems(
    conversations: List<ConversationUi>,
    onOpenConversation: (Long) -> Unit,
    modifier: Modifier,
    onTogglePin: ((Long) -> Unit)? = null,
    onHide: ((Long) -> Unit)? = null,
    onDelete: ((ConversationUi) -> Unit)? = null,
    maxLines: Int = 2,
    onDismiss: ((Long) -> Unit)? = null,
    onMute: (ConversationUi) -> Unit,
    mutedConversations: List<ConversationUi> = emptyList(),
    onUnmute: ((Long) -> Unit)? = null,
) {
    val dismissLabel = stringResource(R.string.panel_dismiss)
    val lines = LocalPreviewLines.current.takeIf { it > 0 } ?: maxLines
    // 앱을 새로 열면 다시 접힌 상태 (같은 화면 안에서는 회전 등에도 유지).
    var mutedExpanded by rememberSaveable { mutableStateOf(false) }
    if (conversations.isEmpty() && mutedConversations.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (onDismiss == null) EmptyConversations()
            else Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(NotiIcons.message, null, Modifier.size(28.dp), tint = NotiColors.Secondary)
                Text(stringResource(R.string.panel_all_read), style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.panel_all_read_hint), color = NotiColors.Secondary,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    } else {
        val panelTranslucent = LocalPanelOpacity.current < 1f
        LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (conversations.isEmpty()) item(key = "empty") {
                EmptyConversations(Modifier.fillMaxWidth())
            }
            items(conversations, key = { it.id }) { conversation ->
                var menuOpen by remember { mutableStateOf(false) }
                val row: @Composable () -> Unit = {
                    Box(Modifier.background(if (panelTranslucent) Color.Transparent else NotiColors.Background)) {
                        ConversationRow(conversation, onClick = { onOpenConversation(conversation.id) },
                            modifier = Modifier.semantics {
                                if (onDismiss != null) customActions = listOf(
                                    CustomAccessibilityAction(dismissLabel) { onDismiss(conversation.id); true })
                            },
                            maxLines = lines, onLongClick = { menuOpen = true },
                            onMute = { onMute(conversation) })
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onDismiss != null) DropdownMenuItem(text = { Text(dismissLabel) },
                                onClick = { menuOpen = false; onDismiss(conversation.id) })
                            if (onTogglePin != null) DropdownMenuItem(
                                text = { Text(stringResource(if (conversation.isPinned) R.string.conversations_unpin else R.string.conversations_pin)) },
                                onClick = { menuOpen = false; onTogglePin(conversation.id) })
                            if (onHide != null) DropdownMenuItem(text = { Text(stringResource(R.string.conversations_hide)) },
                                onClick = { menuOpen = false; onHide(conversation.id) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.conversation_mute)) },
                                onClick = { menuOpen = false; onMute(conversation) })
                            if (onDelete != null) DropdownMenuItem(
                                text = { Text(stringResource(R.string.conversations_delete_history), color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; onDelete(conversation) })
                        }
                    }
                }
                if (onDismiss == null) row()
                else {
                    // A new message can restore the same conversation while this item is composed.
                    key(conversation.id, conversation.lastAt, conversation.preview, conversation.unread) {
                        // 행 폭의 40%를 넘게 밀어야 빠진다 (뒤의 "패널에서 빼기"가 충분히 보인 뒤).
                        val dismissState = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.4f })
                        val dismiss by rememberUpdatedState(onDismiss)
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) dismiss?.invoke(conversation.id)
                        }
                        SwipeToDismissBox(state = dismissState, backgroundContent = {
                            // 글자는 드러나는 쪽 가장자리에 붙여서 조금만 밀어도 바로 보이게 한다.
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            // 반투명 패널에서는 행이 비치므로, 밀고 있을 때만 뒤 배경을 그린다.
                            if (!panelTranslucent || dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                                Box(Modifier.fillMaxSize().background(NotiColors.DetailBackground)
                                    .padding(horizontal = 16.dp), contentAlignment = alignment) {
                                    Text(dismissLabel, color = NotiColors.Secondary,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }) { row() }
                    }
                }
            }
            if (mutedConversations.isNotEmpty()) {
                // 설정 화면과 같은 한 줄: 누를 때마다 펼치기/접기.
                item(key = "muted_toggle") {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = if (mutedExpanded) 4.dp else 0.dp)
                        .clip(NotiShapes.banner).background(NotiColors.DetailBackground)
                        .clickable(role = Role.Button) { mutedExpanded = !mutedExpanded }
                        .heightIn(min = 48.dp).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.conversations_muted_collapsed, mutedConversations.size),
                            Modifier.weight(1f).semantics { heading() },
                            style = MaterialTheme.typography.bodyMedium, color = NotiColors.Body)
                        Text(stringResource(if (mutedExpanded) R.string.conversations_muted_hide else R.string.conversations_muted_show),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            color = NotiColors.NoticeForeground)
                    }
                }
            }
            if (mutedConversations.isNotEmpty() && mutedExpanded) {
                items(mutedConversations, key = { "muted_${it.id}" }) { conversation ->
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        ConversationRow(conversation, onClick = { onOpenConversation(conversation.id) },
                            maxLines = lines, onLongClick = { menuOpen = true })
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onUnmute != null) DropdownMenuItem(
                                text = { Text(stringResource(R.string.conversation_unmute)) },
                                onClick = { menuOpen = false; onUnmute(conversation.id) })
                            if (onDelete != null) DropdownMenuItem(
                                text = { Text(stringResource(R.string.conversations_delete_history), color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; onDelete(conversation) })
                        }
                    }
                }
            }
        }
    }
}

/** 패널 제목 앞의 작은 시크릿노티 아이콘 (런처 아이콘과 같은 그림). */
@Composable
private fun SecretNotiMark() {
    Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF16181D)),
        contentAlignment = Alignment.Center) {
        // 108dp 캔버스 중 그림이 있는 가운데 부분만 22dp 안에 들어오게 크게 그리고 잘라낸다.
        Image(painterResource(R.drawable.ic_noti_foreground), null, Modifier.requiredSize(36.dp))
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) =
    HeaderButton(NotiIcons.sliders, stringResource(R.string.conversations_settings), onClick)

/** 화면 머리의 동그란 아이콘 버튼 (48dp 터치, 옅은 배경). */
@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(NotiSpacing.button).background(NotiColors.DetailBackground, NotiShapes.pill)) {
        Icon(icon, description, Modifier.size(20.dp), tint = NotiColors.Body)
    }
}
