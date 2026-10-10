package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.model.StackedBarEntry
import com.rl04x.koracharts.core.renderer.StackedBarRenderer

/**
 * Custom Android View for rendering Kora Stacked Bar Charts in XML layouts.
 */
public class KoraStackedBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var entries: List<StackedBarEntry> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = StackedBarRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    public var onBarSelectedListener: ((entry: StackedBarEntry?) -> Unit)? = null

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
                R.styleable.KoraStackedBarChartView,
                defStyleAttr,
                0
            )
            val showGrid = a.getBoolean(R.styleable.KoraStackedBarChartView_kora_showGrid, true)
            val showAxisLabels =
                a.getBoolean(R.styleable.KoraStackedBarChartView_kora_showAxisLabels, true)
            val enableZoomXml =
                a.getBoolean(R.styleable.KoraStackedBarChartView_kora_enableZoom, false)
            val animDuration =
                a.getInt(R.styleable.KoraStackedBarChartView_kora_animationDuration, 600)
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
        data: List<StackedBarEntry>,
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
                    val rightPadding = config.paddingDp * density + 10f * density
                    val drawWidth = width.toFloat() - leftPadding - rightPadding

                    val maxPanX = (drawWidth * config.zoomScaleX - drawWidth).coerceAtLeast(0f)
                    val newPanX = (config.panOffsetX - dx).coerceIn(0f, maxPanX)

                    val maxTotal =
                        entries.maxOfOrNull { it.values.sum() }?.coerceAtLeast(1f) ?: 100f
                    val visibleRangeY = maxTotal / config.zoomScaleY
                    val maxPanY = (maxTotal - visibleRangeY).coerceAtLeast(0f)
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

                val absDx = kotlin.math.abs(dx)
                val absDy = kotlin.math.abs(dy)
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
            val rightPadding = config.paddingDp * density + 10f * density
            val drawWidth = width.toFloat() - leftPadding - rightPadding

            val entryCount = entries.size
            if (entryCount > 0 && drawWidth > 0f) {
                val effectiveZoomX = maxOf(1f, config.zoomScaleX)
                val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
                val relativeX = event.x - leftPadding + config.panOffsetX
                val clickedIdx = (relativeX / scaledSlotWidth).toInt()

                if (clickedIdx in 0 until entryCount) {
                    val tapped = entries[clickedIdx]
                    if (config.selectedEntry?.x == tapped.x || config.selectedEntry?.label == tapped.label) {
                        // Tapping same bar closes tooltip
                        config = config.copy(selectedEntry = null)
                        onBarSelectedListener?.invoke(null)
                    } else {
                        config = config.copy(
                            selectedEntry = Entry(
                                tapped.x,
                                tapped.values.sum(),
                                tapped.label
                            )
                        )
                        onBarSelectedListener?.invoke(tapped)
                    }
                } else {
                    // Tapping outside closes tooltip
                    config = config.copy(selectedEntry = null)
                    onBarSelectedListener?.invoke(null)
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
