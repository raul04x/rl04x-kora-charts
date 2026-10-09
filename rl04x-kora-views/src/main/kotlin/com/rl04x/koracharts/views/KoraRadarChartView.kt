package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.RadarDataSet
import com.rl04x.koracharts.core.renderer.RadarRenderer

/**
 * Custom Android View for rendering Kora Radar Charts in XML layouts.
 */
public class KoraRadarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<RadarDataSet> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private val renderer = RadarRenderer()
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.KoraRadarChartView,
                defStyleAttr,
                0
            )
            val animDuration = a.getInt(R.styleable.KoraRadarChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(animationDuration = animDuration.toLong())
        }
    }

    public fun setData(
        data: List<RadarDataSet>,
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
