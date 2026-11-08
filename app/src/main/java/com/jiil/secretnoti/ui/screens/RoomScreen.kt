package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.TargetApps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.components.*
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.MessageUi

@Composable
fun RoomScreen(
    title: String,
    appLabel: String,
    packageName: String,
    isGroup: Boolean,
    messages: List<MessageUi>,
    keywords: List<String>,
    onBack: () -> Unit,
    onOpenInApp: () -> Unit,
    onMute: (deleteExisting: Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var muteDialog by remember { mutableStateOf(false) }
    val ordered = remember(messages) { messages.sortedBy { it.at } }
    // 채팅처럼 최신 메시지(맨 아래)에서 시작한다. 메시지는 DB에서 늦게 채워지므로 채워진 뒤에 내린다.
    // 이후 새 메시지가 오면, 바닥 근처를 보고 있을 때만 따라 내려간다 (지난 대화를 읽는 중이면 그대로).
    val listState = rememberLazyListState()
    var scrolledInitially by remember { mutableStateOf(false) }
    val lastId = ordered.lastOrNull()?.id
    LaunchedEffect(lastId) {
        if (lastId == null) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        val lastIndex = listState.layoutInfo.totalItemsCount - 1
        if (!scrolledInitially) {
            listState.scrollToItem(lastIndex)
            scrolledInitially = true
        } else {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= lastIndex - 3) listState.animateScrollToItem(lastIndex)
        }
    }
    Surface(color = NotiColors.DetailBackground) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().background(NotiColors.Background)
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onBack, Modifier.size(NotiSpacing.button)) {
                    Icon(NotiIcons.chevronLeft, stringResource(R.string.room_screen_back), Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppIcon(packageName, 16.dp)
                        Text(appLabel, color = NotiColors.Secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(NotiIcons.more, stringResource(R.string.conversation_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.conversation_mute)) },
                            onClick = { menuOpen = false; muteDialog = true })
                    }
                }
            }
            HorizontalDivider(color = NotiColors.Divider)
            NoticeBanner(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (ordered.isEmpty()) {
                    item { EmptyConversations(Modifier.fillMaxWidth()) }
                }
                ordered.forEachIndexed { index, message ->
                    val date = messageDate(message.at)
                    if (index == 0 || date != messageDate(ordered[index - 1].at)) {
                        item(key = "date-$date") { DateDivider(date) }
                    }
                    item(key = message.id) { MessageBubble(message, isGroup, keywords = keywords, kakaoEmoticons = packageName == TargetApps.KAKAO) }
                }
            }
            Column(Modifier.background(NotiColors.Background)) {
                HorizontalDivider(color = NotiColors.Divider)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.room_screen_reply_hint, appLabel),
                        Modifier.fillMaxWidth(), color = NotiColors.Secondary,
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    OutlinedButton(onOpenInApp, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = NotiShapes.banner, border = BorderStroke(1.dp, NotiColors.Outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NotiColors.Text)) {
                        Text(stringResource(R.string.room_screen_open, appLabel), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
    if (muteDialog) MuteConversationDialog(title,
        onConfirm = { muteDialog = false; onMute(it) }, onDismiss = { muteDialog = false })

}
