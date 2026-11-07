package com.jiil.secretnoti.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.MotionEvent
import android.view.View
import com.jiil.secretnoti.R
import kotlin.math.abs
import kotlin.math.max

/**
 * 화면 가장자리의 얇은 손잡이. 터치 영역은 뷰 전체(44dp), 보이는 막대는 8dp.
 * 안쪽으로 [openDistance] 이상 밀거나 탭하면 [onOpen]을 부른다.
 */
@SuppressLint("ViewConstructor")
class HandleView(context: Context, private val onOpen: () -> Unit) : View(context) {

    init {
        contentDescription = context.getString(R.string.overlay_handle_description)
    }

    var onLeft = false
        set(v) { field = v; invalidate() }

    var useExclusion = true
        set(v) { field = v; updateExclusion() }

    var badge = 0
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val openDistance = 40 * d
    private val tapSlop = 10 * d

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x6116181D }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2558E8.toInt() }
    private val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * d
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11 * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val bar = RectF()

    private var downX = 0f
    private var downY = 0f
    private var maxInward = 0f
    private var opened = false

    override fun onDraw(canvas: Canvas) {
        val barW = 8 * d
        val left = if (onLeft) 4 * d else width - 4 * d - barW
        bar.set(left, 12 * d, left + barW, height - 12 * d)
        canvas.drawRoundRect(bar, 4 * d, 4 * d, barPaint)

        if (badge > 0) {
            val r = 9 * d
            val cx = bar.centerX()
            val cy = bar.top + r / 2
            canvas.drawCircle(cx, cy, r, badgePaint)
            canvas.drawCircle(cx, cy, r, badgeStroke)
            val label = if (badge > 99) context.getString(R.string.overlay_badge_overflow) else badge.toString()
            canvas.drawText(label, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateExclusion()
    }

    private fun updateExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = if (useExclusion) listOf(Rect(0, 0, width, height)) else emptyList()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                maxInward = 0f
                opened = false
            }
            MotionEvent.ACTION_MOVE -> {
                val inward = if (onLeft) e.rawX - downX else downX - e.rawX
                maxInward = max(maxInward, inward)
                if (!opened && inward > openDistance) {
                    opened = true
                    GestureLog.add(context.getString(R.string.gesture_open_swipe, dp(inward)))
                    onOpen()
                }
            }
            MotionEvent.ACTION_UP -> if (!opened) {
                val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                if (moved < tapSlop && e.eventTime - e.downTime < 400) {
                    GestureLog.add(context.getString(R.string.gesture_open_tap))
                    performClick()
                } else {
                    GestureLog.add(context.getString(R.string.gesture_release_closed, dp(maxInward)))
                }
            }
            MotionEvent.ACTION_CANCEL ->
                GestureLog.add(context.getString(R.string.gesture_cancelled, dp(maxInward)))
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onOpen()
        return true
    }

    private fun dp(px: Float) = (px / d).toInt()
}
