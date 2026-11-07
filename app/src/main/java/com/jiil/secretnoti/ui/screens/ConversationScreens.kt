package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.components.*
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

private fun visibleConversations(conversations: List<ConversationUi>, filter: String?) =
    conversations.filter { filter == null || it.packageName == filter }

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
) {
    var resizing by remember { mutableStateOf(false) }
    val currentWidth by rememberUpdatedState(width)
    val changeWidth by rememberUpdatedState(onWidthChange)
    val finishWidthChange by rememberUpdatedState(onWidthChangeFinished)
    val resizeDescription = stringResource(R.string.panel_resize_description)
    val widenLabel = stringResource(R.string.settings_panel_width_wide)
    val narrowLabel = stringResource(R.string.settings_panel_width_narrow)
    Surface(Modifier.width(width).heightIn(max = 652.dp),
        shape = NotiShapes.panel(onLeftSide), color = NotiColors.Background,
        shadowElevation = 16.dp) {
        Box {
            Column(Modifier.fillMaxSize().padding(
                start = if (onLeftSide) 16.dp else 20.dp,
                end = if (onLeftSide) 20.dp else 16.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.conversations_recent), style = MaterialTheme.typography.titleLarge)
                    NoticeChip()
                }
                AppFilterChips(availableApps, filter, onFilterChange)
                ConversationItems(visibleConversations(conversations, filter).take(10),
                    onOpenConversation, Modifier.weight(1f), maxLines = if (width >= 340.dp) 3 else 2)
                Column {
                    HorizontalDivider(color = NotiColors.Divider)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onShowAll, modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(stringResource(R.string.conversations_all_history), color = NotiColors.NoticeForeground)
                        }
                        SettingsButton(onOpenSettings)
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
                    .pointerInput(onLeftSide, minWidth, maxWidth) {
                        var dragWidth = currentWidth
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragWidth = currentWidth.coerceIn(minWidth, maxWidth)
                                resizing = true
                            },
                            onDragEnd = { resizing = false; finishWidthChange() },
                            onDragCancel = { resizing = false; finishWidthChange() },
                        ) { change, dragAmount ->
                            change.consume()
                            val delta = dragAmount.toDp() * (if (onLeftSide) 1f else -1f)
                            dragWidth = (dragWidth + delta).coerceIn(minWidth, maxWidth)
                            changeWidth(dragWidth)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(4.dp).height(40.dp)
                    .background(if (resizing) NotiColors.Accent else NotiColors.Outline, NotiShapes.pill))
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
) {
    var pendingDelete by remember { mutableStateOf<ConversationUi?>(null) }
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = NotiSpacing.content)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.conversations_all_messages), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onOpenSearch) { Icon(NotiIcons.search, stringResource(R.string.conversations_search)) }
                SettingsButton(onOpenSettings)
            }
            NoticeChip()
            Spacer(Modifier.height(14.dp))
            AppFilterChips(availableApps, filter, onFilterChange)
            Spacer(Modifier.height(8.dp))
            ConversationItems(visibleConversations(conversations, filter), onOpenConversation, Modifier.weight(1f),
                onTogglePin = onTogglePin, onHide = onHide, onDelete = { pendingDelete = it })
        }
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

@Composable
private fun ConversationItems(
    conversations: List<ConversationUi>,
    onOpenConversation: (Long) -> Unit,
    modifier: Modifier,
    onTogglePin: ((Long) -> Unit)? = null,
    onHide: ((Long) -> Unit)? = null,
    onDelete: ((ConversationUi) -> Unit)? = null,
    maxLines: Int = 2,
) {
    if (conversations.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyConversations() }
    } else {
        LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    ConversationRow(conversation, onClick = { onOpenConversation(conversation.id) },
                        maxLines = maxLines,
                        onLongClick = if (onTogglePin != null) ({ menuOpen = true }) else null)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (conversation.isPinned) stringResource(R.string.conversations_unpin) else stringResource(R.string.conversations_pin)) },
                            onClick = { menuOpen = false; onTogglePin?.invoke(conversation.id) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.conversations_hide)) },
                            onClick = { menuOpen = false; onHide?.invoke(conversation.id) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.conversations_delete_history), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete?.invoke(conversation) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(NotiSpacing.button).background(NotiColors.DetailBackground, NotiShapes.pill)) {
        Icon(NotiIcons.sliders, stringResource(R.string.conversations_settings), Modifier.size(20.dp), tint = NotiColors.Body)
    }
}
