package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.BarRenderer

/**
 * Custom Android View for rendering Kora Bar Charts in XML layouts.
 */
public class KoraBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<Dataset> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = BarRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.KoraBarChartView, defStyleAttr, 0)
            val showGrid = a.getBoolean(R.styleable.KoraBarChartView_kora_showGrid, true)
            val showAxes = a.getBoolean(R.styleable.KoraBarChartView_kora_showAxes, true)
            val animDuration = a.getInt(R.styleable.KoraBarChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
                showGrid = showGrid,
                showAxes = showAxes,
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), datasets, config, animProgress)
    }
}
