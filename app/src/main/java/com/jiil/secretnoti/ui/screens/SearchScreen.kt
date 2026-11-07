package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

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
                IconButton(onClick = onBack) { Icon(NotiIcons.chevronLeft, stringResource(R.string.search_screen_back)) }
                TextField(query, onQueryChange, Modifier.weight(1f).focusRequester(focus),
                    placeholder = { Text(stringResource(R.string.search_screen_placeholder)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(NotiIcons.close, stringResource(R.string.search_screen_clear))
                        }
                    })
            }
            if (q.isEmpty() || results.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(if (q.isEmpty()) stringResource(R.string.search_screen_hint)
                        else stringResource(R.string.search_screen_no_results, q), color = NotiColors.Secondary,
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
                                Text(formatRelativeTime(LocalContext.current, result.at), color = NotiColors.Secondary,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            SourceLabel(result.appLabel)
                            Text(highlightedText(if (result.isMine) stringResource(R.string.search_screen_me) else result.sender ?: stringResource(R.string.search_screen_unknown_sender), keywords + q),
                                style = MaterialTheme.typography.labelMedium, color = NotiColors.Secondary)
                            val snippet = remember(result.text, q) { searchSnippet(result.text, q) }
                            Text(highlightedText(snippet, keywords + q), color = NotiColors.Body,
                                style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (result.deletedBySender) MessageTag(stringResource(R.string.search_screen_deleted))
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
