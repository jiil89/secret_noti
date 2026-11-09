package com.jiil.secretnoti.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.jiil.secretnoti.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
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
    val keyboard = LocalSoftwareKeyboardController.current
    val q = query.trim()
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(color = NotiColors.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(NotiSpacing.button)) {
                    Icon(NotiIcons.chevronLeft, stringResource(R.string.search_screen_back), tint = NotiColors.Body)
                }
                TextField(query, onQueryChange, Modifier.weight(1f).padding(end = 12.dp).focusRequester(focus),
                    placeholder = { Text(stringResource(R.string.search_screen_placeholder)) }, singleLine = true,
                    shape = NotiShapes.banner,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NotiColors.DetailBackground,
                        unfocusedContainerColor = NotiColors.DetailBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    leadingIcon = { Icon(NotiIcons.search, null, Modifier.size(20.dp), tint = NotiColors.Secondary) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(NotiIcons.close, stringResource(R.string.search_screen_clear))
                        }
                    })
            }
            if (q.isEmpty() || results.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(NotiIcons.search, null, Modifier.size(28.dp), tint = NotiColors.Secondary)
                        Text(if (q.isEmpty()) stringResource(R.string.search_screen_hint)
                            else stringResource(R.string.search_screen_no_results, q), color = NotiColors.Secondary,
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    itemsIndexed(results, key = { _, it -> it.messageId }) { index, result ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 4.dp), color = NotiColors.Divider)
                        SearchResultRow(result, q, keywords) { onOpenResult(result.conversationId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResultUi, q: String, keywords: List<String>, onClick: () -> Unit) {
    val sender = if (result.isMine) stringResource(R.string.search_screen_me)
        else result.sender ?: stringResource(R.string.search_screen_unknown_sender)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 4.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(result.packageName, 20.dp)
            Text(result.roomTitle, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatRelativeTime(LocalContext.current, result.at), color = NotiColors.Secondary,
                style = MaterialTheme.typography.labelSmall)
        }
        Text(highlightedText(sender, keywords + q), Modifier.padding(start = 28.dp),
            style = MaterialTheme.typography.labelMedium, color = NotiColors.Secondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        val snippet = remember(result.text, q) { searchSnippet(result.text, q) }
        Text(highlightedText(snippet, keywords + q), Modifier.padding(start = 28.dp), color = NotiColors.Body,
            style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (result.deletedBySender) Box(Modifier.padding(start = 28.dp, top = 2.dp)) {
            MessageTag(stringResource(R.string.search_screen_deleted))
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
