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
import com.jiil.secretnoti.ui.NotiTheme
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
    private val dao = NotiApp.instance.db.dumpDao()

    private var handle: HandleView? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private var panelOwner: OverlayOwner? = null
    private val jobs = mutableListOf<Job>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun show() {
        if (handle != null) return
        val view = HandleView(context, onOpen = ::openPanel)
        val params = WindowManager.LayoutParams(
            dp(44), dp(120),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        applyPosition(params)
        wm.addView(view, params)
        handle = view
        handleParams = params

        jobs += scope.launch {
            combine(Prefs.handleOnLeft.flow, Prefs.handleY.flow, Prefs.gestureExclusion.flow) { left, _, exclusion ->
                left to exclusion
            }.collect { (left, exclusion) ->
                view.onLeft = left
                view.useExclusion = exclusion
                applyPosition(params)
                if (view.isAttachedToWindow) wm.updateViewLayout(view, params)
            }
        }
        jobs += scope.launch {
            Prefs.lastPanelOpenedAt.flow
                .flatMapLatest { since -> dao.observePostedSince(since) }
                .collect { view.badge = it }
        }
    }

    fun hide() {
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
                    NotiTheme {
                        PanelContent(onLeft = Prefs.handleOnLeft.value, onClose = ::closePanel)
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

    private fun dp(v: Int) = (v * density).toInt()
}
