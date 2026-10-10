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
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.renderer.PieRenderer

/**
 * Custom Android View for rendering Kora Pie and Donut Charts in XML layouts
 * supporting touch slice selection, explosion animation, and glass tooltip badges.
 */
public class KoraPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var datasets: List<Dataset> = emptyList()
    private var config: ChartConfig = ChartConfig()
    private var holeRadius: Float = 0.55f
    private var renderer = PieRenderer(holeRadius)
    private val animator = LinearAnimator()
    private var animProgress: Float = 1f

    public var onSliceSelectedListener: ((entry: Entry) -> Unit)? = null

    init {
        if (attrs != null) {
            val a =
                context.obtainStyledAttributes(attrs, R.styleable.KoraPieChartView, defStyleAttr, 0)
            val animDuration = a.getInt(R.styleable.KoraPieChartView_kora_animationDuration, 600)
            a.recycle()

            config = config.copy(animationDuration = animDuration.toLong())
        }
    }

    public fun setHoleRadius(ratio: Float) {
        this.holeRadius = ratio
        this.renderer = PieRenderer(holeRadius)
        invalidate()
    }

    public fun setData(
        data: List<Dataset>,
        newConfig: ChartConfig = this.config,
    ) {
        this.datasets = data
        this.config = newConfig
        updateAccessibilityDescription()
        startAnimation()
    }

    private fun updateAccessibilityDescription() {
        val entries = datasets.firstOrNull()?.entries ?: emptyList()
        contentDescription = if (entries.isEmpty()) {
            "Pie chart with no data"
        } else {
            val total = entries.sumOf { it.y.toDouble() }
            "Pie chart with ${entries.size} sectors. Total value: $total."
        }
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
        if (event.actionMasked == MotionEvent.ACTION_UP && datasets.isNotEmpty() && width > 0 && height > 0) {
            val density = resources.displayMetrics.density
            val padding = config.paddingDp * density
            val engine = ChartEngine(width.toFloat(), height.toFloat(), padding)
            val tappedSlice = engine.findPieEntryAt(event.x, event.y, datasets, density)

            if (tappedSlice != null) {
                if (config.selectedEntry == tappedSlice) {
                    config = config.copy(selectedEntry = null)
                } else {
                    config = config.copy(selectedEntry = tappedSlice)
                    onSliceSelectedListener?.invoke(tappedSlice)
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
