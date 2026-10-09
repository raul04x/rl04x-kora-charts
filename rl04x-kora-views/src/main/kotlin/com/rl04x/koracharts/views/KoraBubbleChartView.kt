package com.rl04x.koracharts.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.rl04x.koracharts.core.animation.LinearAnimator
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.renderer.BubbleRenderer

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
            val animDuration = a.getInt(R.styleable.KoraBubbleChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(
                showGrid = showGrid,
                showAxisLabels = showAxisLabels,
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), entries, config, animProgress)
    }
}
