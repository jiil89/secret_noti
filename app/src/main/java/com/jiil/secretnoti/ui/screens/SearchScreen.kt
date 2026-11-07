package com.jiil.secretnoti.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.components.*
import com.jiil.secretnoti.ui.design.*
import com.jiil.secretnoti.ui.model.SearchResultUi

@Composable
fun SearchScreen(
    query: String, onQueryChange: (String) -> Unit, results: List<SearchResultUi>,
    keywords: List<String>, onOpenResult: (conversationId: Long) -> Unit, onBack: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val q = query.trim()
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(NotiIcons.chevronLeft, "뒤로") }
                TextField(query, onQueryChange, Modifier.weight(1f).focusRequester(focus),
                    placeholder = { Text("메시지 검색") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(NotiIcons.close, "검색어 지우기")
                        }
                    })
            }
            if (q.isEmpty() || results.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(if (q.isEmpty()) "보낸 사람·내용으로 찾을 수 있어요"
                        else "‘${q}’와(과) 일치하는 메시지가 없어요", color = NotiColors.Secondary,
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.messageId }) { result ->
                        Column(Modifier.fillMaxWidth().clickable { onOpenResult(result.conversationId) }
                            .padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppIcon(result.packageName, 24.dp)
                                Text(result.roomTitle, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatRelativeTime(result.at), color = NotiColors.Secondary,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            SourceLabel(result.appLabel)
                            Text(highlightedText(if (result.isMine) "나" else result.sender ?: "알 수 없는 보낸 사람", keywords + q),
                                style = MaterialTheme.typography.labelMedium, color = NotiColors.Secondary)
                            val snippet = remember(result.text, q) { searchSnippet(result.text, q) }
                            Text(highlightedText(snippet, keywords + q), color = NotiColors.Body,
                                style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (result.deletedBySender) MessageTag("삭제된 메시지 · 원문 보관됨")
                            HorizontalDivider(Modifier.padding(top = 8.dp), color = NotiColors.Divider)
                        }
                    }
                }
            }
        }
    }
}

internal fun searchSnippet(text: String, query: String): String {
    val match = text.indexOf(query, ignoreCase = true).coerceAtLeast(0)
    var start = (match - 40).coerceAtLeast(0)
    var end = (start + maxOf(180, query.length + 80)).coerceAtMost(text.length)
    if (start > 0 && Character.isLowSurrogate(text[start])) start--
    if (end < text.length && Character.isLowSurrogate(text[end])) end++
    return (if (start > 0) "…" else "") + text.substring(start, end) + if (end < text.length) "…" else ""
}
