package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.components.*
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*

private fun visibleConversations(conversations: List<ConversationUi>, filter: SourceFilter) =
    conversations.filter { filter == SourceFilter.ALL || it.source.name == filter.name }
        .sortedByDescending { it.lastAt }

@Composable
fun EdgePanel(
    conversations: List<ConversationUi>,
    availableSources: Set<Source>,
    filter: SourceFilter,
    onFilterChange: (SourceFilter) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onShowAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onLeftSide: Boolean,
) {
    Surface(Modifier.width(NotiSpacing.panelWidth).heightIn(max = 652.dp),
        shape = NotiShapes.panel(onLeftSide), color = NotiColors.Background,
        shadowElevation = 16.dp) {
        Box {
            Column(Modifier.fillMaxSize().padding(
                start = if (onLeftSide) 16.dp else 20.dp,
                end = if (onLeftSide) 20.dp else 16.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("최근 메시지", style = MaterialTheme.typography.titleLarge)
                    NoticeChip()
                }
                SourceFilterChips(availableSources, filter, onFilterChange)
                ConversationItems(visibleConversations(conversations, filter).take(10),
                    onOpenConversation, Modifier.weight(1f))
                Column {
                    HorizontalDivider(color = NotiColors.Divider)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onShowAll, modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("모든 기록 보기", color = NotiColors.NoticeForeground)
                        }
                        SettingsButton(onOpenSettings)
                    }
                }
            }
            Box(Modifier.align(if (onLeftSide) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 8.dp).width(4.dp).height(48.dp)
                .background(NotiColors.Outline, NotiShapes.pill))
        }
    }
}

@Composable
fun ConversationListScreen(
    conversations: List<ConversationUi>,
    availableSources: Set<Source>,
    filter: SourceFilter,
    onFilterChange: (SourceFilter) -> Unit,
    onOpenConversation: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = NotiSpacing.content)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("모든 메시지", style = MaterialTheme.typography.titleLarge)
                SettingsButton(onOpenSettings)
            }
            NoticeChip()
            Spacer(Modifier.height(14.dp))
            SourceFilterChips(availableSources, filter, onFilterChange)
            Spacer(Modifier.height(8.dp))
            ConversationItems(visibleConversations(conversations, filter), onOpenConversation, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConversationItems(
    conversations: List<ConversationUi>,
    onOpenConversation: (Long) -> Unit,
    modifier: Modifier,
) {
    if (conversations.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyConversations() }
    } else {
        LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationRow(conversation, onClick = { onOpenConversation(conversation.id) })
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(NotiSpacing.button).background(NotiColors.DetailBackground, NotiShapes.pill)) {
        Icon(NotiIcons.sliders, "설정", Modifier.size(20.dp), tint = NotiColors.Body)
    }
}
