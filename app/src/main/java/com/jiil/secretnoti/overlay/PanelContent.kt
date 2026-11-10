package com.jiil.secretnoti.overlay

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.apps
import com.jiil.secretnoti.ui.design.LocalPanelOpacity
import com.jiil.secretnoti.ui.model.PanelWidth
import com.jiil.secretnoti.ui.screens.EdgePanel
import com.jiil.secretnoti.widget.WidgetUpdater
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 패널 창의 모서리 반경(dp). 창 배경(흐림 영역)과 카드 모양을 같게 맞춘다. */
const val PANEL_CORNER_DP = 28

/**
 * 엣지패널 창의 내용: 카드 하나. 창 크기가 곧 카드 크기이고, 배경색·흐림은 창이 그린다.
 * 바깥을 누르거나 뒤로 가기를 누르면 창(Dialog)이 닫힌다.
 */
@Composable
fun PanelContent(
    onLeft: Boolean,
    onOpenConversation: (Long) -> Unit,
    onShowAll: () -> Unit,
    onOpenSettings: () -> Unit,
    /** 패널 배경 불투명도 (1 = 불투명). */
    opacity: Float = 1f,
    /** 폭을 끄는 동안 창 폭도 맞춰 바꾼다. */
    onWidthChange: (Dp) -> Unit = {},
    /** 패널 최대 높이. 대화방이 적으면 그만큼 낮아진다. */
    maxHeight: Dp = 560.dp,
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

    CompositionLocalProvider(LocalPanelOpacity provides opacity) {
        EdgePanel(
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
            onWidthChange = { width = it; onWidthChange(it) },
            onWidthChangeFinished = { Prefs.panelWidthDp.value = width.value.roundToInt() },
            onDismiss = { id -> scope.launch { chat.dismissFromPanel(id) } },
            onDismissAll = { scope.launch { chat.dismissAllFromPanel() } },
            onMute = { id, deleteExisting ->
                scope.launch { chat.mute(id, deleteExisting); WidgetUpdater.request(context) }
            },
            shape = RoundedCornerShape(PANEL_CORNER_DP.dp),
            drawBackground = false,
            wrapHeight = true,
            maxHeight = maxHeight,
        )
    }
}
