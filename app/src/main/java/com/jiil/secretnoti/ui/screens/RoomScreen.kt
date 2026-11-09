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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    participants: List<String> = emptyList(),
    messages: List<MessageUi>,
    keywords: List<String>,
    onBack: () -> Unit,
    onOpenInApp: () -> Unit,
    onMute: (deleteExisting: Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var muteDialog by rememberSaveable { mutableStateOf(false) }
    val ordered = remember(messages) { messages.sortedBy { it.at } }
    val firstInRunIds = remember(ordered) {
        ordered.filterIndexed { index, message ->
            val previous = ordered.getOrNull(index - 1)
            previous == null || previous.sender != message.sender || previous.isMine != message.isMine ||
                messageDate(previous.at) != messageDate(message.at)
        }.mapTo(mutableSetOf()) { it.id }
    }
    // 채팅 앱처럼 아래에서부터 쌓는다(reverseLayout). 0번 항목이 최신 메시지라 처음부터 맨 아래가 보이고,
    // 위쪽 사진이 늦게 불러와져 높이가 바뀌어도 아래(최신)는 그대로 붙어 있다.
    val listState = rememberLazyListState()
    val lastId = ordered.lastOrNull()?.id
    LaunchedEffect(lastId) {
        // 새 메시지가 오면, 바닥 근처를 보고 있을 때만 따라 내려간다 (지난 대화를 읽는 중이면 그대로).
        if (lastId != null && listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
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
                        val subtitle = if (isGroup) {
                            val group = stringResource(R.string.room_screen_group_subtitle, appLabel)
                            if (participants.isEmpty()) group
                            else stringResource(R.string.room_screen_group_participants, group, participants.joinToString(", "))
                        } else appLabel
                        Text(subtitle, color = NotiColors.Secondary, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(NotiSpacing.button)) {
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
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)) {
                if (ordered.isEmpty()) {
                    item { EmptyConversations(Modifier.fillMaxWidth()) }
                }
                // 최신부터 넣는다. 날짜 구분선은 그날의 가장 오래된 메시지 다음에 넣어야 화면에서 그날 메시지들 위에 온다.
                val newestFirst = ordered.asReversed()
                newestFirst.forEachIndexed { index, message ->
                    item(key = message.id) { MessageBubble(message, isGroup, keywords = keywords, kakaoEmoticons = packageName == TargetApps.KAKAO, firstInRun = message.id in firstInRunIds) }
                    val date = messageDate(message.at)
                    val older = newestFirst.getOrNull(index + 1)
                    if (older == null || messageDate(older.at) != date) {
                        item(key = "date-$date") { DateDivider(date) }
                    }
                }
            }
            Column(Modifier.background(NotiColors.Background)) {
                HorizontalDivider(color = NotiColors.Divider)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.room_screen_reply_hint, appLabel),
                        Modifier.fillMaxWidth(), color = NotiColors.Secondary,
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    OutlinedButton(onOpenInApp, Modifier.fillMaxWidth().heightIn(min = NotiSpacing.button),
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
