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

/** 가장자리 손잡이와 엣지패널 오버레이 창을 관리한다. 메인 스레드에서만 호출. */
class EdgeOverlayController(private val context: Context, private val scope: CoroutineScope) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val chat = NotiApp.instance.chat

    private var handle: HandleView? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
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
                    NotiTheme(Prefs.themeMode.value) {
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

        val owner = OverlayOwner().also { it.start() }
        val root = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) closePanel()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.setViewTreeViewModelStoreOwner(owner)
        root.addView(
            ComposeView(context).apply {
                setContent {
                    NotiTheme(Prefs.themeMode.value) {
                        PanelContent(
                            onLeft = Prefs.handleOnLeft.value,
                            onClose = ::closePanel,
                            onOpenConversation = { openApp(MainActivity.routeRoom(it)) },
                            onShowAll = { openApp(MainActivity.ROUTE_LIST) },
                            onOpenSettings = { openApp(MainActivity.ROUTE_SETTINGS) },
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        // 뒤로 가기 키를 받으려면 포커스를 가져야 하므로 FLAG_NOT_FOCUSABLE을 넣지 않는다.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        wm.addView(root, params)
        panel = root
        panelOwner = owner
        handle?.visibility = View.GONE
    }

    private fun closePanel() {
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
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
