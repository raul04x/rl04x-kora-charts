package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.LineRenderer

/**
 * Custom Android View for rendering Kora Line Charts in XML layouts.
 */
public class KoraLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<Dataset> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = LineRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f
    private var isCurvedXml: Boolean = false
    private var gradientFillXml: Boolean = false

    public var onPointSelectedListener: ((x: Float, y: Float) -> Unit)? = null

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
                R.styleable.KoraLineChartView,
                defStyleAttr,
                0
            )
            val showGrid = a.getBoolean(R.styleable.KoraLineChartView_kora_showGrid, true)
            val showAxes = a.getBoolean(R.styleable.KoraLineChartView_kora_showAxes, true)
            val showLegend = a.getBoolean(R.styleable.KoraLineChartView_kora_showLegend, true)
            val showAxisLabels =
                a.getBoolean(R.styleable.KoraLineChartView_kora_showAxisLabels, true)
            isCurvedXml = a.getBoolean(R.styleable.KoraLineChartView_kora_isCurved, false)
            gradientFillXml = a.getBoolean(R.styleable.KoraLineChartView_kora_gradientFill, false)
            val enableZoomXml = a.getBoolean(R.styleable.KoraLineChartView_kora_enableZoom, false)
            val animDuration = a.getInt(R.styleable.KoraLineChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
                showGrid = showGrid,
                showAxes = showAxes,
                showLegend = showLegend,
                showAxisLabels = showAxisLabels,
                enableZoom = enableZoomXml,
                animationDuration = animDuration.toLong(),
            )
        }
    }

    public fun setData(
        data: List<Dataset>,
        newConfig: ChartConfig = this.config,
    ) {
        this.datasets = data.map { ds ->
            ds.copy(
                isCurved = if (isCurvedXml) true else ds.isCurved,
                gradientFill = if (gradientFillXml) true else ds.gradientFill,
            )
        }
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
                    val padding = config.paddingDp * density
                    val engine = ChartEngine(width.toFloat(), height.toFloat(), padding)
                    val range = engine.computeRange(datasets)

                    val visibleRangeX = range.rangeX / config.zoomScaleX
                    val maxPanX = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
                    val deltaDataX = -dx / width.toFloat() * visibleRangeX
                    val newPanX = (config.panOffsetX + deltaDataX).coerceIn(0f, maxPanX)

                    val visibleRangeY = range.rangeY / config.zoomScaleY
                    val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
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

                val absDx = Math.abs(dx)
                val absDy = Math.abs(dy)
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

        if (datasets.isNotEmpty() && width > 0 && height > 0) {
            val density = resources.displayMetrics.density
            val padding = config.paddingDp * density
            val engine = ChartEngine(width.toFloat(), height.toFloat(), padding)
            val range = engine.computeRange(datasets)
            val nearest = engine.nearestEntry(event.x, event.y, datasets, range)
            if (nearest != null && nearest != config.selectedEntry) {
                config = config.copy(selectedEntry = nearest)
                onPointSelectedListener?.invoke(nearest.x, nearest.y)
                performClick()
                invalidate()
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), datasets, config, animProgress)
    }
}
