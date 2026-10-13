package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.renderer.BubbleRenderer
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Custom Android View for rendering Kora Bubble Scatter Plot Charts in XML layouts.
 */
public class KoraBubbleChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var entries: List<BubbleEntry> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = BubbleRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    public var onBubbleSelectedListener: ((bubble: BubbleEntry?) -> Unit)? = null

    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!config.enableZoom) return false
                val newZoomX = (config.zoomScaleX * detector.scaleFactor).coerceIn(1f, 5f)
                val newZoomY = (config.zoomScaleY * detector.scaleFactor).coerceIn(1f, 5f)
                config = config.copy(zoomScaleX = newZoomX, zoomScaleY = newZoomY)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
        })

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.KoraBubbleChartView,
                defStyleAttr,
                0
            )
            val showGrid = a.getBoolean(R.styleable.KoraBubbleChartView_kora_showGrid, true)
            val showAxisLabels =
                a.getBoolean(R.styleable.KoraBubbleChartView_kora_showAxisLabels, true)
            val enableZoomXml =
                a.getBoolean(R.styleable.KoraBubbleChartView_kora_enableZoom, false)
            val animDuration = a.getInt(R.styleable.KoraBubbleChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
                showGrid = showGrid,
                showAxisLabels = showAxisLabels,
                enableZoom = enableZoomXml,
                animationDuration = animDuration.toLong(),
            )
        }
    }

    public fun setData(
        data: List<BubbleEntry>,
        newConfig: ChartConfig = this.config,
    ) {
        this.entries = data
        this.config = newConfig
        startAnimation()
    }

    private fun startAnimation() {
        animator.start(config.animationDuration) { progress ->
            animProgress = progress
            postInvalidate()
        }
    }

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDraggingChart = false

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (config.enableZoom) {
            scaleGestureDetector.onTouchEvent(event)
            if (event.pointerCount > 1) {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }

        val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isDraggingChart = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY

                if (config.enableZoom && (config.zoomScaleX > 1.05f || config.zoomScaleY > 1.05f)) {
                    val density = resources.displayMetrics.density
                    val leftPadding =
                        if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                    val rightPadding = config.paddingDp * density + 18f * density
                    val drawWidth = (width.toFloat() - leftPadding - rightPadding).coerceAtLeast(1f)

                    val maxPanX = (drawWidth * config.zoomScaleX - drawWidth).coerceAtLeast(0f)
                    val newPanX = (config.panOffsetX - dx).coerceIn(0f, maxPanX)

                    val maxY = entries.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
                    val visibleRangeY = maxY / config.zoomScaleY
                    val maxPanY = (maxY - visibleRangeY).coerceAtLeast(0f)
                    val deltaDataY = dy / height.toFloat() * visibleRangeY
                    val newPanY = (config.panOffsetY + deltaDataY).coerceIn(0f, maxPanY)

                    if (newPanX != config.panOffsetX || newPanY != config.panOffsetY) {
                        config = config.copy(panOffsetX = newPanX, panOffsetY = newPanY)
                        touchDownX = event.x
                        touchDownY = event.y
                        parent?.requestDisallowInterceptTouchEvent(true)
                        invalidate()
                        return true
                    }
                }

                val absDx = abs(dx)
                val absDy = abs(dy)
                if (!isDraggingChart && absDx > touchSlop && absDx > absDy * 1.2f) {
                    isDraggingChart = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                } else if (!isDraggingChart && absDy > touchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingChart = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        if (entries.isNotEmpty() && width > 0 && height > 0 && event.actionMasked == MotionEvent.ACTION_UP && !isDraggingChart) {
            val density = resources.displayMetrics.density
            val leftPadding =
                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
            val rightPadding = config.paddingDp * density + 18f * density
            val bottomPadding =
                if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
            val topPadding = config.paddingDp * density + 10f * density
            val topHeadroom = 32f * density

            val drawWidth = width.toFloat() - leftPadding - rightPadding
            val drawHeight = height.toFloat() - topPadding - bottomPadding - topHeadroom

            val maxX = entries.maxOfOrNull { it.x }?.coerceAtLeast(1f) ?: 1f
            val maxY = entries.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
            val baselineY = topPadding + topHeadroom + drawHeight

            val count = entries.size
            if (count > 0 && drawWidth > 0f) {
                var nearest: BubbleEntry? = null
                var minDist = Float.MAX_VALUE

                val visibleZoomY = maxOf(1f, config.zoomScaleY)
                val visibleRangeY = if (visibleZoomY == 1f) maxY else maxY / visibleZoomY
                val clampedPanY =
                    config.panOffsetY.coerceIn(0f, (maxY - visibleRangeY).coerceAtLeast(0f))

                for (bubble in entries) {
                    val pctX = bubble.x / maxX
                    val cx = leftPadding + (pctX * drawWidth * maxOf(
                        1f,
                        config.zoomScaleX
                    )) - config.panOffsetX
                    val pctY = (bubble.y - clampedPanY) / visibleRangeY
                    val cy = baselineY - (pctY * drawHeight)

                    val dx = event.x - cx
                    val dy = event.y - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val threshold = maxOf(bubble.radiusDp * density, 24f * density)

                    if (dist < minDist && dist <= threshold) {
                        minDist = dist
                        nearest = bubble
                    }
                }

                if (nearest != null) {
                    if (config.selectedEntry?.x == nearest.x && config.selectedEntry?.y == nearest.y) {
                        config = config.copy(selectedEntry = null)
                        onBubbleSelectedListener?.invoke(null)
                    } else {
                        config =
                            config.copy(selectedEntry = Entry(nearest.x, nearest.y, nearest.label))
                        onBubbleSelectedListener?.invoke(nearest)
                    }
                } else {
                    config = config.copy(selectedEntry = null)
                    onBubbleSelectedListener?.invoke(null)
                }
                performClick()
                invalidate()
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), entries, config, animProgress)
    }
}
