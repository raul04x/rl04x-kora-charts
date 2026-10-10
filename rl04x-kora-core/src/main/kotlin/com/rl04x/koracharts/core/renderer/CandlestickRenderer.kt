package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import java.util.Locale

/**
 * Renderer for financial Candlestick charts with zero allocations in draw().
 */
public class CandlestickRenderer : BaseRenderer<CandlestickEntry> {

    private val axisGridRenderer = AxisGridRenderer()

    private val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val candlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bullColor = "#10B981".toColorInt()
    private val bearColor = "#EF4444".toColorInt()

    private val cachedBodyRect = RectF()

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<CandlestickEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val bottomPadding =
            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
        val topPadding = config.paddingDp * density + 26f * density
        val rightPadding = config.paddingDp * density + 10f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight
        val minVal = data.minOf { it.low }
        val maxVal = data.maxOf { it.high }

        val tickResult = axisGridRenderer.drawYAxisGridAndLabels(
            canvas = canvas,
            config = config,
            minVal = minVal,
            maxVal = maxVal,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            density = density,
        )

        val effectiveMinVal = tickResult?.niceMin ?: minVal
        val effectiveMaxVal = tickResult?.niceMax ?: maxVal
        val effectiveValRange = (effectiveMaxVal - effectiveMinVal).coerceAtLeast(1f)

        val count = data.size
        val slotWidth = drawWidth / count
        val candleWidth = (slotWidth * 0.55f).coerceAtLeast(4f * density)

        for ((idx, entry) in data.withIndex()) {
            val cx = leftPadding + (idx * slotWidth) + (slotWidth / 2f)
            val isBullish = entry.close >= entry.open
            val color = if (isBullish) bullColor else bearColor

            wickPaint.color = color
            candlePaint.color = color

            fun toY(v: Float): Float {
                return topPadding + (1f - (v - effectiveMinVal) / effectiveValRange) * drawHeight
            }

            val highY = toY(entry.high)
            val lowY = toY(entry.low)
            val openY = toY(entry.open)
            val closeY = toY(entry.close)

            canvas.drawLine(cx, highY, cx, lowY, wickPaint)

            val topBody = minOf(openY, closeY)
            val bottomBody = maxOf(openY, closeY)
            val bodyHeight = (bottomBody - topBody).coerceAtLeast(2f * density)

            val left = cx - (candleWidth / 2f)
            val right = cx + (candleWidth / 2f)
            cachedBodyRect.set(left, topBody, right, topBody + bodyHeight * progress)
            canvas.drawRoundRect(cachedBodyRect, 2f * density, 2f * density, candlePaint)

            if (config.showAxisLabels) {
                val labelText = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                axisGridRenderer.drawXAxisLabel(
                    canvas = canvas,
                    label = labelText,
                    cx = cx,
                    baselineY = baselineY,
                    rotationDeg = config.xAxisLabelRotation,
                    config = config,
                    density = density,
                )
            }
        }
    }
}
