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
import androidx.compose.ui.graphics.Color
import com.jiil.secretnoti.capture.KakaoEmoticons
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.*
import java.time.LocalDate

@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    background: Color = NotiColors.Avatar,
    foreground: Color = NotiColors.Body,
) {
    val initial = name.trim().let { if (it.isEmpty()) "?" else String(Character.toChars(it.codePointAt(0))) }
    Box(modifier.size(44.dp).background(background, CircleShape)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(initial, color = foreground,
            style = MaterialTheme.typography.titleSmall.copy(lineHeightStyle = SingleLineCentered))
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
            style = MaterialTheme.typography.labelSmall.copy(lineHeightStyle = SingleLineCentered),
            fontWeight = FontWeight.Bold, maxLines = 1)
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
                    // 칩 높이 36dp 안에서 글자를 위아래 정확히 가운데에 둔다.
                    Box(Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (selected) NotiColors.Background else NotiColors.Body,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeightStyle = SingleLineCentered),
                            fontWeight = FontWeight.Medium, maxLines = 1)
                    }
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
        Text(stringResource(R.string.messages_no_read_receipt), color = NotiColors.NoticeForeground,
            style = MaterialTheme.typography.labelMedium.copy(lineHeightStyle = SingleLineCentered))
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationRow(
    conversation: ConversationUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    maxLines: Int = 2,
    onMute: (() -> Unit)? = null,
) {
    Row(modifier.alpha(if (conversation.isMuted) 0.55f else 1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick,
            onLongClickLabel = stringResource(R.string.messages_manage_conversation), role = Role.Button)
        .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            // 채널은 단톡으로 표시돼 와도 채널로 보인다 (사람 대화와 색으로 구분).
            if (conversation.isChannel) {
                InitialAvatar(conversation.title, background = NotiColors.ChannelBackground,
                    foreground = NotiColors.ChannelForeground)
            } else if (conversation.isGroup) {
                Box(Modifier.size(44.dp).background(NotiColors.NoticeBackground, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(NotiIcons.users, stringResource(R.string.messages_group), Modifier.size(24.dp),
                        tint = NotiColors.NoticeForeground)
                }
            } else InitialAvatar(conversation.title)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                val recordsGone = conversation.isMuted && !conversation.hasRecords
                val dimmed = recordsGone || conversation.previewHidden
                val me = stringResource(R.string.me)
                // 단톡은 "보낸 사람: 내용"으로, 보낸 사람만 굵게. 번역 문구의 %1$s 자리를 찾아 그 부분만 강조한다.
                val senderTemplate = stringResource(R.string.messages_sender_preview, "\u0000", "\u0001")
                val preview = when {
                    recordsGone -> AnnotatedString(stringResource(R.string.conversation_records_deleted))
                    conversation.previewHidden -> AnnotatedString(stringResource(R.string.preview_locked))
                    conversation.isGroup && !conversation.isChannel -> senderPreview(senderTemplate,
                        conversation.lastSender?.takeIf { it.isNotBlank() } ?: me, conversation.preview)
                    else -> AnnotatedString(conversation.preview)
                }
                Text(preview, Modifier.weight(1f), color = if (dimmed) NotiColors.Secondary else NotiColors.Body,
                    style = MaterialTheme.typography.bodyMedium, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
                UnreadBadge(conversation.unread, Modifier.padding(top = 1.dp))
            }
            // 꼬리표가 많아 한 줄을 넘기면 다음 줄로 넘긴다 (좁은 패널에서도 잘리지 않게).
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically) {
                SourceLabel(conversation.appLabel)
                if (conversation.isChannel) {
                    MessageTag(stringResource(R.string.conversation_channel_tag), TagTone.Channel)
                } else if (conversation.isGroup && conversation.senderCount >= 2) {
                    MessageTag(pluralStringResource(R.plurals.messages_group_senders,
                        conversation.senderCount, conversation.senderCount))
                }
                if (conversation.isMuted) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(NotiIcons.saveOff, null, Modifier.size(12.dp), tint = NotiColors.Secondary)
                        MessageTag(stringResource(R.string.conversation_saving_off))
                    }
                }
                if (conversation.deletedCount > 0) MessageTag(pluralStringResource(R.plurals.messages_deleted_count, conversation.deletedCount, conversation.deletedCount))
                if (conversation.hasKeyword) MessageTag(stringResource(R.string.messages_keyword), TagTone.Accent)
            }
            if (!conversation.isMuted && conversation.looksLikeAd && !conversation.isChannel && onMute != null) {
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
    firstInRun: Boolean = true,
) {
    val deletedOutline = NotiColors.DeletedOutline
    val incomingGroup = isGroup && !message.isMine
    val shape = when {
        message.isMine -> NotiShapes.mine
        incomingGroup && !firstInRun -> RoundedCornerShape(18.dp)
        else -> NotiShapes.bubble
    }
    val background = when {
        message.deletedBySender -> NotiColors.DeletedBackground
        message.isMine -> NotiColors.NoticeBackground
        else -> NotiColors.Background
    }
    Row(modifier.fillMaxWidth()) {
        if (incomingGroup) {
            if (firstInRun) InitialAvatar(message.sender ?: stringResource(R.string.messages_unknown_sender), Modifier.size(34.dp))
            else Spacer(Modifier.width(34.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (incomingGroup && firstInRun) {
                Text(message.sender ?: stringResource(R.string.messages_unknown_sender), color = NotiColors.Body,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                if (!message.isMine) {
                    Spacer(Modifier.width(6.dp))
                    Text(formatClockTime(message.at), color = NotiColors.Secondary,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun DateDivider(date: LocalDate, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(formatDateLabel(LocalContext.current, date), Modifier.background(NotiColors.Divider, NotiShapes.pill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
            color = NotiColors.Secondary,
            style = MaterialTheme.typography.bodySmall.copy(lineHeightStyle = SingleLineCentered))
    }
}

/** "%1$s: %2$s" 같은 번역 문구에 보낸 사람·내용을 넣고, 보낸 사람 부분만 굵게 만든다. */
private fun senderPreview(template: String, sender: String, text: String): AnnotatedString = buildAnnotatedString {
    val s = template.indexOf('\u0000')
    val t = template.indexOf('\u0001')
    if (s < 0 || t < 0) {
        append(template.replace("\u0000", sender).replace("\u0001", text))
        return@buildAnnotatedString
    }
    var i = 0
    for ((pos, isSender) in listOf(s to true, t to false).sortedBy { it.first }) {
        append(template.substring(i, pos))
        if (isSender) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(sender) } else append(text)
        i = pos + 1
    }
    append(template.substring(i))
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

enum class TagTone { Neutral, Accent, Channel }

/** 대화방 행·검색 결과의 작은 꼬리표. 한 줄, 글자는 위아래 정가운데. */
@Composable
fun MessageTag(text: String, tone: TagTone = TagTone.Neutral) {
    val (bg, fg) = when (tone) {
        TagTone.Neutral -> NotiColors.DetailBackground to NotiColors.Secondary
        TagTone.Accent -> NotiColors.NoticeBackground to NotiColors.NoticeForeground
        TagTone.Channel -> NotiColors.ChannelBackground to NotiColors.ChannelForeground
    }
    Text(text, Modifier.background(bg, NotiShapes.pill).padding(horizontal = 6.dp, vertical = 2.dp),
        color = fg, maxLines = 1,
        fontWeight = if (tone == TagTone.Channel) FontWeight.Bold else null,
        style = MaterialTheme.typography.labelSmall.copy(lineHeightStyle = SingleLineCentered))
}
