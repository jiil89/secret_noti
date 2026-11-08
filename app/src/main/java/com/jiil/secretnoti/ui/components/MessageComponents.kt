package com.jiil.secretnoti.ui.components

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.KakaoEmoticons
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val description = pluralStringResource(R.plurals.messages_unread, count, count)
    Box(modifier.heightIn(min = 20.dp).widthIn(min = 20.dp)
        .background(NotiColors.Accent, NotiShapes.pill)
        .padding(horizontal = 6.dp, vertical = 2.dp)
        .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center) {
        Text(if (count > 99) stringResource(R.string.messages_badge_overflow) else count.toString(), color = NotiColors.OnAccent,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SourceLabel(appLabel: String, modifier: Modifier = Modifier) {
    Text(appLabel, modifier, color = NotiColors.Secondary, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun AppFilterChips(
    availableApps: List<AppRef>, filter: String?, onFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableApps.size < 2) return
    val filters = listOf(null to stringResource(R.string.messages_filter_all)) + availableApps.map { it.packageName to it.label }
    Row(modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { (packageName, label) ->
            val selected = packageName == filter
            Box(Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                .selectable(selected, onClick = { onFilterChange(packageName) }, role = Role.Tab),
                contentAlignment = Alignment.Center) {
                Surface(shape = NotiShapes.pill,
                    color = if (selected) NotiColors.Text else NotiColors.Background,
                    border = if (selected) null else BorderStroke(1.dp, NotiColors.Outline)) {
                    Text(label, Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (selected) NotiColors.Background else NotiColors.Body,
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
        Text(stringResource(R.string.messages_no_read_receipt), color = NotiColors.NoticeForeground, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NoticeBanner(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(NotiColors.NoticeBackground, NotiShapes.banner)
        .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(NotiIcons.eyeOff, null, Modifier.size(16.dp), tint = NotiColors.NoticeForeground)
        Text(stringResource(R.string.messages_notice),
            color = NotiColors.NoticeForeground, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationRow(
    conversation: ConversationUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    maxLines: Int = 2,
    onMute: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick,
            onLongClickLabel = stringResource(R.string.messages_manage_conversation), role = Role.Button)
        .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            InitialAvatar(conversation.title)
            AppIcon(conversation.packageName, 18.dp,
                Modifier.align(Alignment.BottomEnd)
                    .background(NotiColors.Background, CircleShape).padding(2.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (conversation.isPinned) Icon(NotiIcons.pin, stringResource(R.string.messages_pinned), Modifier.size(14.dp), tint = NotiColors.Secondary)
                Text(conversation.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatRelativeTime(LocalContext.current, conversation.lastAt), style = MaterialTheme.typography.labelSmall,
                    color = NotiColors.Secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val preview = if (conversation.previewHidden) stringResource(R.string.preview_locked)
                else if (conversation.isGroup)
                    stringResource(R.string.messages_sender_preview, conversation.lastSender?.takeIf { it.isNotBlank() } ?: stringResource(R.string.me), conversation.preview)
                else conversation.preview
                Text(preview, Modifier.weight(1f), color = if (conversation.previewHidden) NotiColors.Secondary else NotiColors.Body,
                    style = MaterialTheme.typography.bodyMedium, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
                UnreadBadge(conversation.unread)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SourceLabel(conversation.appLabel, Modifier.weight(1f, fill = false))
                if (conversation.deletedCount > 0) MessageTag(pluralStringResource(R.plurals.messages_deleted_count, conversation.deletedCount, conversation.deletedCount))
                if (conversation.hasKeyword) MessageTag(stringResource(R.string.messages_keyword), accent = true)
            }
            if (conversation.looksLikeAd && onMute != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.conversation_looks_like_ad), Modifier.weight(1f),
                        color = NotiColors.Secondary, style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = onMute, contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.conversation_mute), color = NotiColors.Secondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageUi,
    isGroup: Boolean,
    modifier: Modifier = Modifier,
    keywords: List<String> = emptyList(),
    kakaoEmoticons: Boolean = false,
) {
    val deletedOutline = NotiColors.DeletedOutline
    val shape = if (message.isMine) NotiShapes.mine else NotiShapes.bubble
    val background = when {
        message.deletedBySender -> NotiColors.DeletedBackground
        message.isMine -> NotiColors.NoticeBackground
        else -> NotiColors.Background
    }
    Column(modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isGroup && !message.isMine) {
            Text(message.sender ?: stringResource(R.string.messages_unknown_sender), color = NotiColors.Secondary,
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
                    drawPath(path, deletedOutline, style = stroke)
                }
            } else Modifier
            Column(Modifier.weight(1f, fill = false).widthIn(max = if (message.deletedBySender) 270.dp else 250.dp)
                .then(border).background(background, shape).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (message.deletedBySender) {
                    Text(stringResource(R.string.messages_deleted_original_saved), color = NotiColors.Secondary,
                        style = MaterialTheme.typography.labelMedium)
                }
                // The UI contract supplies preserved original content in text.
                val sticker = KakaoEmoticons.isSticker(message.text)
                message.imagePath?.let { MessagePhoto(it, sticker = sticker) }
                val placeholder = message.text.trim().removeSuffix(".") == "사진을 보냈습니다" || sticker
                if (message.imagePath == null || !placeholder) {
                    Text(messageText(message.text, kakaoEmoticons, keywords),
                        color = if (message.deletedBySender) NotiColors.Body else NotiColors.Text,
                        style = MaterialTheme.typography.bodyLarge)
                }
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
        Text(formatDateLabel(LocalContext.current, date), Modifier.background(NotiColors.Divider, NotiShapes.pill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
            color = NotiColors.Secondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmptyConversations(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(NotiIcons.message, null, Modifier.size(32.dp), tint = NotiColors.Secondary)
        Text(stringResource(R.string.messages_empty_title), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.messages_empty_body), color = NotiColors.Secondary,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun MessageTag(text: String, accent: Boolean = false) {
    Text(text, Modifier.background(if (accent) NotiColors.NoticeBackground else NotiColors.DetailBackground,
        NotiShapes.pill).padding(horizontal = 6.dp, vertical = 2.dp),
        color = if (accent) NotiColors.NoticeForeground else NotiColors.Secondary,
        style = MaterialTheme.typography.labelSmall)
}
