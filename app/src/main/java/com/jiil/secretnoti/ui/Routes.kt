package com.jiil.secretnoti.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.AppLauncher
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.AppRef
import com.jiil.secretnoti.ui.model.ConversationUi
import com.jiil.secretnoti.ui.screens.ConversationListScreen
import com.jiil.secretnoti.ui.screens.RoomScreen
import com.jiil.secretnoti.ui.screens.SearchScreen
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/** 대화방 목록에 보이는 앱들 (필터 칩용). */
fun List<ConversationUi>.apps(): List<AppRef> =
    distinctBy { it.packageName }.map { AppRef(it.packageName, it.appLabel) }

/** 저장소와 디자인 화면을 잇는 연결부. */
@Composable
fun ConversationListRoute(onOpenConversation: (Long) -> Unit, onOpenSettings: () -> Unit, onOpenSearch: () -> Unit) {
    val context = LocalContext.current
    val chat = remember { NotiApp.instance.chat }
    val scope = rememberCoroutineScope()
    val conversations by remember { chat.observeConversations() }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val apps = conversations.apps()
    // 필터로 고른 앱의 대화방이 사라지면 전체로 돌아간다.
    if (filter != null && apps.none { it.packageName == filter }) filter = null

    ConversationListScreen(
        conversations = conversations,
        availableApps = apps,
        filter = filter,
        onFilterChange = { filter = it },
        onOpenConversation = onOpenConversation,
        onOpenSettings = onOpenSettings,
        onOpenSearch = onOpenSearch,
        onTogglePin = { id -> scope.launch { chat.togglePin(id); WidgetUpdater.request(context) } },
        onHide = { id -> scope.launch { chat.setHidden(id, true); WidgetUpdater.request(context) } },
        onDelete = { id -> scope.launch { chat.deleteConversation(id); WidgetUpdater.request(context) } },
    )
}

@Composable
fun RoomRoute(conversationId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val chat = remember { NotiApp.instance.chat }
    val conversation by remember(conversationId) { chat.observeConversation(conversationId) }
        .collectAsStateWithLifecycle(null)
    val messages by remember(conversationId) { chat.observeMessages(conversationId) }
        .collectAsStateWithLifecycle(emptyList())
    val keywords by Prefs.keywords.flow.collectAsStateWithLifecycle()

    // 이 앱 안에서 본 것으로 표시 (카톡·Teams의 읽음과는 무관).
    LaunchedEffect(conversationId, messages.size) {
        chat.markSeen(conversationId)
        WidgetUpdater.request(context)
    }

    val conv = conversation ?: return
    RoomScreen(
        title = conv.title,
        appLabel = conv.appLabel,
        packageName = conv.packageName,
        isGroup = conv.isGroup,
        messages = messages,
        keywords = keywords.sorted(),
        onBack = onBack,
        onOpenInApp = {
            if (!AppLauncher.openConversation(context, conv.id, conv.packageName)) {
                Toast.makeText(context, context.getString(R.string.app_not_found, conv.appLabel), Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@Composable
fun SearchRoute(onOpenConversation: (Long) -> Unit, onBack: () -> Unit) {
    val chat = remember { NotiApp.instance.chat }
    var query by rememberSaveable { mutableStateOf("") }
    val queryFlow = remember { MutableStateFlow("") }
    LaunchedEffect(query) { queryFlow.value = query }
    val results by remember { queryFlow.debounce(200).flatMapLatest { chat.search(it) } }
        .collectAsStateWithLifecycle(emptyList())
    val keywords by Prefs.keywords.flow.collectAsStateWithLifecycle()

    SearchScreen(
        query = query,
        onQueryChange = { query = it },
        results = results,
        keywords = keywords.sorted(),
        onOpenResult = onOpenConversation,
        onBack = onBack,
    )
}
