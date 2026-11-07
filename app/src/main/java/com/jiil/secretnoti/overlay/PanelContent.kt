package com.jiil.secretnoti.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jiil.secretnoti.ui.design.NotiColors
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.ui.apps
import com.jiil.secretnoti.ui.screens.EdgePanel

/** 오버레이 창 전체: 반투명 배경 + 가장자리 엣지패널 카드. 바깥을 누르면 닫힌다. */
@Composable
fun PanelContent(
    onLeft: Boolean,
    onClose: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onShowAll: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val chat = remember { NotiApp.instance.chat }
    val conversations by remember { chat.observeConversations() }.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(NotiColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Box(
            Modifier
                .align(if (onLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(vertical = 96.dp)
                // 카드 안을 눌렀을 때는 닫히지 않도록 탭을 소비한다.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            EdgePanel(
                conversations = conversations,
                availableApps = conversations.apps(),
                filter = filter,
                onFilterChange = { filter = it },
                onOpenConversation = onOpenConversation,
                onShowAll = onShowAll,
                onOpenSettings = onOpenSettings,
                onLeftSide = onLeft,
            )
        }
    }
}
