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
import com.jiil.secretnoti.ui.design.LocalPanelOpacity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.launch
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.PanelWidth
import kotlin.math.roundToInt
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
    /** 패널 배경 불투명도 (1 = 불투명). */
    opacity: Float = 1f,
    /** 창 뒤 흐림이 켜져 있으면 어둡게 하는 막을 옅게 한다. */
    blurBehind: Boolean = false,
) {
    val chat = remember { NotiApp.instance.chat }
    val conversations by remember { chat.observePanelConversations() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var filter by remember { mutableStateOf<String?>(null) }

    // 폭: 끄는 동안은 화면에만 반영하고, 손을 떼면 저장한다.
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val maxWidth = (screenWidth - PanelWidth.SCREEN_MARGIN_DP).coerceAtLeast(PanelWidth.MIN_DP)
    var width by remember { mutableStateOf(Prefs.panelWidthDp.value.coerceIn(PanelWidth.MIN_DP, maxWidth).dp) }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (blurBehind) NotiColors.Scrim.copy(alpha = NotiColors.Scrim.alpha * 0.4f) else NotiColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Box(
            Modifier
                .align(if (onLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(vertical = 96.dp)
                // 카드 안을 눌렀을 때는 닫히지 않도록 탭을 소비한다.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            CompositionLocalProvider(LocalPanelOpacity provides opacity) { EdgePanel(
                conversations = conversations,
                availableApps = conversations.apps(),
                filter = filter,
                onFilterChange = { filter = it },
                onOpenConversation = onOpenConversation,
                onShowAll = onShowAll,
                onOpenSettings = onOpenSettings,
                onLeftSide = onLeft,
                width = width,
                minWidth = PanelWidth.MIN_DP.dp,
                maxWidth = maxWidth.dp,
                onWidthChange = { width = it },
                onWidthChangeFinished = { Prefs.panelWidthDp.value = width.value.roundToInt() },
                onDismiss = { id -> scope.launch { chat.dismissFromPanel(id) } },
                onDismissAll = { scope.launch { chat.dismissAllFromPanel() } },
                onMute = { id, deleteExisting ->
                    scope.launch { chat.mute(id, deleteExisting); WidgetUpdater.request(context) }
                },
            ) }
        }
    }
}
