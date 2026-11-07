package com.jiil.secretnoti.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.capture.AppLauncher
import com.jiil.secretnoti.ui.model.SourceFilter
import com.jiil.secretnoti.ui.screens.ConversationListScreen
import com.jiil.secretnoti.ui.screens.RoomScreen

/** 저장소와 디자인 화면을 잇는 연결부. */
@Composable
fun ConversationListRoute(onOpenConversation: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val chat = remember { NotiApp.instance.chat }
    val conversations by remember { chat.observeConversations() }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf(SourceFilter.ALL) }
    ConversationListScreen(
        conversations = conversations,
        availableSources = conversations.map { it.source }.toSet(),
        filter = filter,
        onFilterChange = { filter = it },
        onOpenConversation = onOpenConversation,
        onOpenSettings = onOpenSettings,
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

    // 이 앱 안에서 본 것으로 표시 (카톡·Teams의 읽음과는 무관).
    LaunchedEffect(conversationId, messages.size) { chat.markSeen(conversationId) }

    val conv = conversation ?: return
    RoomScreen(
        title = conv.title,
        source = conv.source,
        packageName = conv.packageName,
        isGroup = conv.isGroup,
        messages = messages,
        onBack = onBack,
        onOpenInApp = {
            if (!AppLauncher.openConversation(context, conv.id, conv.packageName)) {
                Toast.makeText(context, "${conv.source.label} 앱을 찾을 수 없어요", Toast.LENGTH_SHORT).show()
            }
        },
    )
}
