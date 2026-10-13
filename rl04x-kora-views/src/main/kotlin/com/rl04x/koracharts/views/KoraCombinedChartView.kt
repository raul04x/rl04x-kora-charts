package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.CombinedRenderer

/**
 * Custom Android View for rendering Kora Combined Bar + Line Overlay Charts in XML layouts with touch point selection.
 */
public class KoraCombinedChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<Dataset> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = CombinedRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    public var onPointSelectedListener: ((x: Float, y: Float) -> Unit)? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.KoraCombinedChartView,
                defStyleAttr,
                0
            )
            val showGrid = a.getBoolean(R.styleable.KoraCombinedChartView_kora_showGrid, true)
            val showAxisLabels =
                a.getBoolean(R.styleable.KoraCombinedChartView_kora_showAxisLabels, true)
            val animDuration =
                a.getInt(R.styleable.KoraCombinedChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
                showGrid = showGrid,
                showAxisLabels = showAxisLabels,
                enableZoom = false,
                animationDuration = animDuration.toLong(),
            )
        } else {
            config = config.copy(enableZoom = false)
        }
    }

    public fun setData(
        data: List<Dataset>,
        newConfig: ChartConfig = this.config,
    ) {
        this.datasets = data
        this.config = newConfig.copy(enableZoom = false)
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
        if (datasets.isNotEmpty() && width > 0 && height > 0 && event.actionMasked == MotionEvent.ACTION_UP) {
            val density = resources.displayMetrics.density
            val engine = ChartEngine(width.toFloat(), height.toFloat(), config.paddingDp * density)
            val nearest = engine.nearestLineEntry(event.x, event.y, datasets, config, density)

            if (nearest != null) {
                config = if (config.selectedEntry == nearest) {
                    config.copy(selectedEntry = null)
                } else {
                    onPointSelectedListener?.invoke(nearest.x, nearest.y)
                    config.copy(selectedEntry = nearest)
                }
                performClick()
                invalidate()
            } else {
                if (config.selectedEntry != null) {
                    config = config.copy(selectedEntry = null)
                    performClick()
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), datasets, config, animProgress)
    }
}
