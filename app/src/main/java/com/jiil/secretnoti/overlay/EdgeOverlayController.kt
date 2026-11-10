package com.jiil.secretnoti.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.BadgeStyle
import com.jiil.secretnoti.ui.model.PANEL_TRANSPARENCY_MAX
import com.jiil.secretnoti.ui.model.PanelWidth
import com.jiil.secretnoti.ui.model.ThemeMode
import com.jiil.secretnoti.ui.design.NotiDarkPalette
import com.jiil.secretnoti.ui.design.NotiLightPalette
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import com.jiil.secretnoti.ui.MainActivity
import com.jiil.secretnoti.ui.NotiTheme
import com.jiil.secretnoti.ui.components.PeekBubble
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private const val PANEL_MAX_HEIGHT_DP = 652
private const val PANEL_VERTICAL_MARGIN_DP = 96
/** 패널을 화면 가장자리에서 띄우는 거리. */
private const val PANEL_EDGE_MARGIN_DP = 8

/** 가장자리 손잡이와 엣지패널 오버레이 창을 관리한다. 메인 스레드에서만 호출. */
class EdgeOverlayController(private val context: Context, private val scope: CoroutineScope) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val chat = NotiApp.instance.chat

    private var handle: HandleView? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panel: Dialog? = null
    private var panelOwner: OverlayOwner? = null
    private val jobs = mutableListOf<Job>()
    private var peek: View? = null
    private var peekOwner: OverlayOwner? = null
    private var peekJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun show() {
        if (handle != null) return
        val view = HandleView(context, onOpen = ::openPanel)
        val params = WindowManager.LayoutParams(
            dp(44), dp(Prefs.handleSize.value.heightDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        applyPosition(params)
        wm.addView(view, params)
        handle = view
        handleParams = params

        jobs += scope.launch {
            combine(
                Prefs.handleOnLeft.flow, Prefs.handleY.flow, Prefs.handleSize.flow, Prefs.gestureExclusion.flow,
            ) { left, _, size, exclusion -> Triple(left, size, exclusion) }.collect { (left, size, exclusion) ->
                view.onLeft = left
                view.useExclusion = exclusion
                params.height = dp(size.heightDp)
                applyPosition(params)
                if (view.isAttachedToWindow) wm.updateViewLayout(view, params)
            }
        }
        jobs += scope.launch {
            Prefs.lastPanelOpenedAt.flow
                .flatMapLatest { since -> chat.observeIncomingSince(since) }
                .collect { view.badge = it }
        }
        jobs += scope.launch { Prefs.indicatorBadge.flow.collect { view.showBadge = it } }
        jobs += scope.launch { Prefs.badgeStyle.flow.collect { view.dotBadge = it == BadgeStyle.DOT } }
        jobs += scope.launch { OverlayEvents.newMessages.collect(::onNewMessage) }
        jobs += scope.launch { OverlayEvents.glowPreviews.collect { view.glow(Prefs.glowColor.value) } }
    }

    private fun onNewMessage(event: NewMessageEvent) {
        val view = handle ?: return
        if (panel != null) return
        if (Prefs.indicatorGlow.value) {
            val color = if (Prefs.glowUsesAppColor.value) {
                AppColors.of(context, event.packageName, Prefs.glowColor.value)
            } else {
                Prefs.glowColor.value
            }
            view.glow(color)
        }
        if (Prefs.peekBubble.value && !AppVisibility.mainVisible) showPeek(event)
    }

    /** 손잡이 옆에 보낸 사람과 첫 줄을 3초 동안 띄운다. 누르면 패널이 열린다. */
    private fun showPeek(event: NewMessageEvent) {
        hidePeek()
        val hp = handleParams ?: return
        val owner = OverlayOwner().also { it.start() }
        val onLeft = Prefs.handleOnLeft.value
        val root = FrameLayout(context)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.setViewTreeViewModelStoreOwner(owner)
        root.addView(
            ComposeView(context).apply {
                setContent {
                    NotiTheme(Prefs.themeMode.value, Prefs.textSize.value, Prefs.previewLines.value) {
                        PeekBubble(event.title, event.text, event.packageName, onLeft) {
                            hidePeek()
                            openPanel()
                        }
                    }
                }
            },
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = hp.gravity
            x = hp.width
            y = hp.y + hp.height / 2 - dp(28)
        }
        if (runCatching { wm.addView(root, params) }.isFailure) {
            owner.destroy()
            return
        }
        peek = root
        peekOwner = owner
        peekJob = scope.launch {
            delay(3000)
            hidePeek()
        }
    }

    private fun hidePeek() {
        peekJob?.cancel()
        peekJob = null
        peek?.let { runCatching { wm.removeView(it) } }
        peek = null
        peekOwner?.destroy()
        peekOwner = null
    }

    fun hide() {
        hidePeek()
        closePanel()
        jobs.forEach { it.cancel() }
        jobs.clear()
        handle?.let { runCatching { wm.removeView(it) } }
        handle = null
        handleParams = null
    }

    private fun applyPosition(p: WindowManager.LayoutParams) {
        p.gravity = (if (Prefs.handleOnLeft.value) Gravity.LEFT else Gravity.RIGHT) or Gravity.TOP
        val screenH = context.resources.displayMetrics.heightPixels
        p.x = 0
        p.y = ((screenH - p.height) * Prefs.handleY.value).toInt()
    }

    private fun openPanel() {
        if (panel != null) return
        hidePeek()
        Prefs.lastPanelOpenedAt.value = System.currentTimeMillis()

        // 패널은 카드 크기의 창 하나다. 창 배경을 반투명으로 칠하고 그 뒤만 흐리게 하므로
        // (Window.setBackgroundBlurRadius) 화면의 나머지는 어둡게 하지도, 흐리게 하지도 않는다.
        // 창 뒤 흐림은 Window 객체에만 있어서 Dialog로 띄운다. 바깥 터치·뒤로 가기는 Dialog가 닫는다.
        val owner = OverlayOwner().also { it.start() }
        val onLeft = Prefs.handleOnLeft.value
        // 시스템이 '흐림 꺼짐'이라고 알려도 일단 요청한다. 지원하지 않으면 아무 일도 일어나지 않는다.
        val blur = Prefs.panelBlur.value && PanelBlur.supported
        val opacity = 1f - Prefs.panelTransparency.value.coerceIn(0, PANEL_TRANSPARENCY_MAX) / 100f
        val screenWidthDp = context.resources.configuration.screenWidthDp
        val maxWidthDp = (screenWidthDp - PanelWidth.SCREEN_MARGIN_DP).coerceAtLeast(PanelWidth.MIN_DP)
        val widthDp = Prefs.panelWidthDp.value.coerceIn(PanelWidth.MIN_DP, maxWidthDp)
        // 위쪽 끝은 예전(바 사이 화면 가운데에 위아래 96dp 여백, 최대 652dp)과 같게 두고,
        // 높이만 그 2/3로 줄여 아래쪽을 올린다.
        val usable = usableScreenHeight()
        val fullHeightPx = minOf(dp(PANEL_MAX_HEIGHT_DP), usable - dp(PANEL_VERTICAL_MARGIN_DP * 2))
        val topPx = (usable - fullHeightPx) / 2
        val heightPx = fullHeightPx * 2 / 3
        val maxHeightDp = (heightPx / density).toInt()

        val dialog = Dialog(context, android.R.style.Theme_Material_Dialog_NoActionBar)
        val content = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                NotiTheme(Prefs.themeMode.value, Prefs.textSize.value, Prefs.previewLines.value) {
                    PanelContent(
                        onLeft = onLeft,
                        opacity = opacity,
                        maxHeight = maxHeightDp.dp,
                        onWidthChange = { w -> dialog.window?.setLayout(dp(w.value.roundToInt()), heightPx) },
                        onOpenConversation = { openApp(MainActivity.routeRoom(it)) },
                        onShowAll = { openApp(MainActivity.ROUTE_LIST) },
                        onOpenSettings = { openApp(MainActivity.ROUTE_SETTINGS) },
                    )
                }
            }
        }
        dialog.setContentView(content)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { if (panel === dialog) closePanel() }
        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setFormat(PixelFormat.TRANSLUCENT)
            // 흐림 영역의 모서리는 창 배경의 둥근 모서리를 따른다.
            setBackgroundDrawable(GradientDrawable().apply {
                cornerRadius = dp(PANEL_CORNER_DP).toFloat()
                setColor(panelBackground(opacity))
            })
            decorView.setPadding(0, 0, 0, 0)
            setGravity((if (onLeft) Gravity.LEFT else Gravity.RIGHT) or Gravity.TOP)
            attributes = attributes.also { it.x = dp(PANEL_EDGE_MARGIN_DP); it.y = topPx }
            setLayout(dp(widthDp), heightPx)
            setWindowAnimations(0)
            // 반투명일 때 그림자는 패널 안으로 비쳐 탁해 보이므로 뺀다 (대신 얇은 테두리).
            setElevation(if (opacity < 1f) 0f else dp(16).toFloat())
            if (blur) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setBackgroundBlurRadius(dp(PanelBlur.RADIUS_DP))
                // 갤럭시는 일반 흐림을 막아 둔 경우가 있어 삼성 흐림도 시도한다.
                PanelBlur.trySamsungBlur(decorView, dp(PanelBlur.RADIUS_DP), dp(PANEL_CORNER_DP).toFloat())
            }
        }
        runCatching { dialog.show() }.onFailure {
            owner.destroy()
            return
        }
        panel = dialog
        panelOwner = owner
        handle?.visibility = View.GONE
    }

    /**
     * 상태바·내비게이션 바를 뺀 화면 높이(px). 예전 전체 화면 창이 바 사이에서만 그려졌으므로
     * 같은 높이가 나오도록 바 높이를 뺀다.
     */
    private fun usableScreenHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val metrics = wm.currentWindowMetrics
                val bars = metrics.windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
                return metrics.bounds.height() - bars.top - bars.bottom
            }
        }
        return context.resources.displayMetrics.heightPixels
    }

    /** 앱 테마(밝게·어둡게·시스템)에 맞춘 패널 배경색. */
    private fun panelBackground(opacity: Float): Int {
        val dark = when (Prefs.themeMode.value) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        return (if (dark) NotiDarkPalette else NotiLightPalette).Background.copy(alpha = opacity).toArgb()
    }

    private fun closePanel() {
        val dialog = panel ?: return
        panel = null
        runCatching { dialog.dismiss() }
        panelOwner?.destroy()
        panelOwner = null
        handle?.visibility = View.VISIBLE
    }

    private fun openApp(route: String) {
        closePanel()
        context.startActivity(MainActivity.intent(context, route))
    }

    private fun dp(v: Int) = (v * density).toInt()
}
