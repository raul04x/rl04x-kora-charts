package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.HorizontalBarRenderer

/**
 * Custom Android View for rendering Kora Horizontal Bar Charts in XML layouts.
 */
public class KoraHorizontalBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<Dataset> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = HorizontalBarRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    public var onBarSelectedListener: ((dataset: Dataset?) -> Unit)? = null

    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!config.enableZoom) return false
                val newZoomX = (config.zoomScaleX * detector.scaleFactor).coerceIn(1f, 5f)
                config = config.copy(zoomScaleX = newZoomX)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
        })

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.KoraHorizontalBarChartView,
                defStyleAttr,
                0
            )
            val showAxisLabels =
                a.getBoolean(R.styleable.KoraHorizontalBarChartView_kora_showAxisLabels, true)
            val enableZoomXml =
                a.getBoolean(R.styleable.KoraHorizontalBarChartView_kora_enableZoom, false)
            val animDuration =
                a.getInt(R.styleable.KoraHorizontalBarChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
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
        this.datasets = data
        this.config = newConfig
        startAnimation()
    }

    private fun startAnimation() {
        animator.start(config.animationDuration) { progress ->
            animProgress = progress
            postInvalidate()
        }
    }

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

        if (datasets.isNotEmpty() && width > 0 && height > 0 && event.actionMasked == MotionEvent.ACTION_UP) {
            val density = resources.displayMetrics.density
            val topPadding = config.paddingDp * density
            val bottomPadding = config.paddingDp * density
            val drawHeight = height.toFloat() - topPadding - bottomPadding

            val count = datasets.size
            if (count > 0 && drawHeight > 0f) {
                val slotHeight = drawHeight / count
                val relativeY = event.y - topPadding
                val clickedIdx = (relativeY / slotHeight).toInt()

                if (clickedIdx in 0 until count) {
                    val tapped = datasets[clickedIdx]
                    val tappedEntry = tapped.entries.firstOrNull()
                    if (config.selectedEntry == tappedEntry) {
                        config = config.copy(selectedEntry = null)
                        onBarSelectedListener?.invoke(null)
                    } else {
                        config = config.copy(selectedEntry = tappedEntry)
                        onBarSelectedListener?.invoke(tapped)
                    }
                } else {
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
        renderer.draw(canvas, width.toFloat(), height.toFloat(), datasets, config, animProgress)
    }
}
