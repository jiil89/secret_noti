package com.jiil.secretnoti.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.animation.ValueAnimator
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.jiil.secretnoti.R
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

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

    /** 숫자 배지를 보여줄지 (설정 → 새 메시지 표시). */
    var showBadge = true
        set(v) { field = v; invalidate() }

    /** 배지를 숫자 대신 작은 점으로. */
    var dotBadge = false
        set(v) { field = v; invalidate() }

    private val badgeRect = RectF()
    private val textBounds = Rect()

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowRect = RectF()
    private var glowColor = 0
    private var glowLevel = 0f
    private var glowAnimator: ValueAnimator? = null

    init {
        // 번지는 빛(BlurMaskFilter)은 소프트웨어 레이어에서만 그려진다. 손잡이는 작아서 부담이 없다.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /** 메시지가 오면 [color]로 세 번 은은하게 빛나고 멈춘다 (약 4초). */
    fun glow(color: Int) {
        glowColor = color or 0xFF000000.toInt()
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0f, 3f).apply {
            duration = 4200
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = (it.animatedValue as Float) % 1f
                glowLevel = sin(t * PI).toFloat()
                invalidate()
            }
            doOnEnd { glowLevel = 0f; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        glowAnimator?.cancel()
        super.onDetachedFromWindow()
    }

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
        textAlign = Paint.Align.LEFT
    }
    private val bar = RectF()

    private var downX = 0f
    private var downY = 0f
    private var opened = false

    override fun onDraw(canvas: Canvas) {
        val barW = 8 * d
        val left = if (onLeft) 4 * d else width - 4 * d - barW
        bar.set(left, 12 * d, left + barW, height - 12 * d)
        if (glowLevel > 0f) {
            val spread = 4 * d + 6 * d * glowLevel
            glowRect.set(bar.left - spread / 2, bar.top - spread / 2, bar.right + spread / 2, bar.bottom + spread / 2)
            glowPaint.color = glowColor
            glowPaint.alpha = (90 + 140 * glowLevel).toInt()
            glowPaint.maskFilter = BlurMaskFilter(6 * d + 8 * d * glowLevel, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(glowRect, 8 * d, 8 * d, glowPaint)
            glowPaint.maskFilter = null
            glowPaint.alpha = 255
            canvas.drawRoundRect(bar, 4 * d, 4 * d, glowPaint)
        } else {
            canvas.drawRoundRect(bar, 4 * d, 4 * d, barPaint)
        }

        if (showBadge && badge > 0) drawBadge(canvas)
    }

    /**
     * 배지는 막대 위쪽, 화면 안쪽 가장자리에 반쯤 걸친다 (왼쪽 손잡이면 오른쪽으로, 오른쪽이면 왼쪽으로).
     * 숫자는 글자 실제 크기로 가운데를 잡고, 자릿수가 늘면 알약이 화면 안쪽으로만 늘어난다.
     */
    private fun drawBadge(canvas: Canvas) {
        val inner = if (onLeft) bar.right else bar.left
        val stroke = badgeStroke.strokeWidth
        if (dotBadge) {
            val r = 5 * d
            val cx = if (onLeft) inner + r - 2 * d else inner - r + 2 * d
            val cy = bar.top + r
            canvas.drawCircle(cx, cy, r, badgePaint)
            canvas.drawCircle(cx, cy, r, badgeStroke)
            return
        }
        val label = if (badge > 99) context.getString(R.string.overlay_badge_overflow) else badge.toString()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val h = 18 * d
        val w = maxOf(h, textBounds.width() + 10 * d)
        // 막대 안쪽 가장자리에서 6dp만 걸치고 나머지는 화면 안쪽으로. 뷰(44dp) 밖으로 나가지 않게 막는다.
        val overlap = 6 * d
        var start = if (onLeft) inner - overlap else inner + overlap - w
        start = start.coerceIn(stroke, width - w - stroke)
        val top = bar.top - 2 * d
        badgeRect.set(start, top, start + w, top + h)
        val radius = h / 2
        canvas.drawRoundRect(badgeRect, radius, radius, badgePaint)
        canvas.drawRoundRect(badgeRect, radius, radius, badgeStroke)
        // 글자 상자의 실제 가운데를 알약 가운데에 맞춘다 (글꼴 위아래 여백 때문에 생기는 쏠림 방지).
        canvas.drawText(label, badgeRect.centerX() - textBounds.exactCenterX(), badgeRect.centerY() - textBounds.exactCenterY(), textPaint)
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
                opened = false
            }
            MotionEvent.ACTION_MOVE -> {
                val inward = if (onLeft) e.rawX - downX else downX - e.rawX
                if (!opened && inward > openDistance) {
                    opened = true
                    onOpen()
                }
            }
            MotionEvent.ACTION_UP -> if (!opened) {
                val moved = abs(e.rawX - downX) + abs(e.rawY - downY)
                if (moved < tapSlop && e.eventTime - e.downTime < 400) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onOpen()
        return true
    }

}
