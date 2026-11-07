package com.jiil.secretnoti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.capture.Source
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*
import java.time.LocalDate

@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier) {
    val initial = name.trim().let { if (it.isEmpty()) "?" else String(Character.toChars(it.codePointAt(0))) }
    Box(modifier.size(44.dp).background(NotiColors.Avatar, CircleShape)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(initial, color = NotiColors.Body, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(modifier.heightIn(min = 20.dp).widthIn(min = 20.dp)
        .background(NotiColors.Accent, NotiShapes.pill)
        .padding(horizontal = 6.dp, vertical = 2.dp)
        .clearAndSetSemantics { contentDescription = "이 앱에서 확인하지 않은 메시지 ${count}개" },
        contentAlignment = Alignment.Center) {
        Text(if (count > 99) "99+" else count.toString(), color = Color.White,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SourceLabel(source: Source, modifier: Modifier = Modifier) {
    Text(source.label, modifier, color = NotiColors.Secondary, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun SourceFilterChips(
    availableSources: Set<Source>,
    filter: SourceFilter,
    onFilterChange: (SourceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(SourceFilter.ALL) + Source.entries
        .filter { it in availableSources }.map { SourceFilter.valueOf(it.name) }
    Row(modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { item ->
            val selected = item == filter
            val label = when (item) {
                SourceFilter.ALL -> "전체"
                SourceFilter.KAKAO -> "카톡"
                SourceFilter.SMS -> "문자"
                SourceFilter.TEAMS -> "Teams"
            }
            Box(Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                .selectable(selected, onClick = { onFilterChange(item) }, role = Role.Tab),
                contentAlignment = Alignment.Center) {
                Surface(shape = NotiShapes.pill,
                    color = if (selected) NotiColors.Text else Color.White,
                    border = if (selected) null else BorderStroke(1.dp, NotiColors.Outline)) {
                    Text(label, Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (selected) Color.White else NotiColors.Body,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun NoticeChip(modifier: Modifier = Modifier) {
    Row(modifier.background(NotiColors.NoticeBackground, NotiShapes.pill)
        .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(NotiIcons.eyeOff, null, Modifier.size(14.dp), tint = NotiColors.NoticeForeground)
        Text("읽음 표시 안 남음", color = NotiColors.NoticeForeground, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NoticeBanner(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(NotiColors.NoticeBackground, NotiShapes.banner)
        .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(NotiIcons.eyeOff, null, Modifier.size(16.dp), tint = NotiColors.NoticeForeground)
        Text("알림으로 받은 내용이라 상대방에게 읽음 표시가 안 남아요",
            color = NotiColors.NoticeForeground, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ConversationRow(
    conversation: ConversationUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick, role = Role.Button)
        .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InitialAvatar(conversation.title)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(conversation.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatRelativeTime(conversation.lastAt), style = MaterialTheme.typography.labelSmall,
                    color = NotiColors.Secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val preview = if (conversation.isGroup)
                    "${conversation.lastSender?.takeIf { it.isNotBlank() } ?: "나"}: ${conversation.preview}"
                else conversation.preview
                Text(preview, Modifier.weight(1f), color = NotiColors.Body,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                UnreadBadge(conversation.unread)
            }
            SourceLabel(conversation.source)
        }
    }
}

@Composable
fun MessageBubble(message: MessageUi, isGroup: Boolean, modifier: Modifier = Modifier) {
    val shape = if (message.isMine) NotiShapes.mine else NotiShapes.bubble
    val background = when {
        message.deletedBySender -> NotiColors.DeletedBackground
        message.isMine -> NotiColors.NoticeBackground
        else -> Color.White
    }
    Column(modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isGroup && !message.isMine) {
            Text(message.sender ?: "알 수 없는 보낸 사람", color = NotiColors.Secondary,
                style = MaterialTheme.typography.labelMedium)
        }
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom) {
            if (message.isMine) {
                Text(formatClockTime(message.at), color = NotiColors.Secondary,
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
            }
            val border = if (message.deletedBySender) Modifier.drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this) as Outline.Rounded
                val path = Path().apply { addRoundRect(outline.roundRect) }
                val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                onDrawWithContent {
                    drawContent()
                    drawPath(path, NotiColors.DeletedOutline, style = stroke)
                }
            } else Modifier
            Column(Modifier.weight(1f, fill = false).widthIn(max = if (message.deletedBySender) 270.dp else 250.dp)
                .then(border).background(background, shape).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (message.deletedBySender) {
                    Text("삭제된 메시지 · 원문 보관됨", color = NotiColors.Secondary,
                        style = MaterialTheme.typography.labelMedium)
                }
                // The UI contract supplies preserved original content in text.
                Text(message.text, color = if (message.deletedBySender) NotiColors.Body else NotiColors.Text,
                    style = MaterialTheme.typography.bodyLarge)
            }
            if (!message.isMine && !message.deletedBySender) {
                Spacer(Modifier.width(6.dp))
                Text(formatClockTime(message.at), color = NotiColors.Secondary,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DateDivider(date: LocalDate, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(formatDateLabel(date), Modifier.background(NotiColors.Divider, NotiShapes.pill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
            color = NotiColors.Secondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmptyConversations(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(NotiIcons.message, null, Modifier.size(32.dp), tint = NotiColors.Secondary)
        Text("아직 받은 메시지가 없어요", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text("새 알림이 오면 여기에 모아 보여드려요.", color = NotiColors.Secondary,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
