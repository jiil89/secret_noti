package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
) {
    val ordered = remember(messages) { messages.sortedBy { it.at } }
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
            }
            HorizontalDivider(color = NotiColors.Divider)
            NoticeBanner(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (ordered.isEmpty()) {
                    item { EmptyConversations(Modifier.fillMaxWidth()) }
                }
                ordered.forEachIndexed { index, message ->
                    val date = messageDate(message.at)
                    if (index == 0 || date != messageDate(ordered[index - 1].at)) {
                        item(key = "date-$date") { DateDivider(date) }
                    }
                    item(key = message.id) { MessageBubble(message, isGroup, keywords = keywords) }
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
}
