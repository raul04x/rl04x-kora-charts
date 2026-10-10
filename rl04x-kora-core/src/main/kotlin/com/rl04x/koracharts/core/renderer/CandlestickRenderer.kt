package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.ChartConfig
import java.util.Locale

/**
 * Renderer for financial Candlestick charts.
 */
public class CandlestickRenderer : BaseRenderer<CandlestickEntry> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val candlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val bullColor = "#10B981".toColorInt() // Bullish green
    private val bearColor = "#EF4444".toColorInt() // Bearish red

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
        gridPaint.color = config.style.gridColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 8f * density

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
        val valRange = (maxVal - minVal).coerceAtLeast(1f)

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = minVal,
                max = maxVal,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMinVal = yTickResult?.niceMin ?: minVal
        val effectiveMaxVal = yTickResult?.niceMax ?: maxVal
        val effectiveValRange = (effectiveMaxVal - effectiveMinVal).coerceAtLeast(1f)

        // Draw horizontal grid lines
        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val pct = (yVal - effectiveMinVal) / effectiveValRange
                    val y = topPadding + (1f - pct) * drawHeight
                    if (y in (topPadding - 1f)..(baselineY + 1f)) {
                        canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                        if (config.showAxisLabels) {
                            val labelText = config.yAxisFormatter?.invoke(yVal)
                                ?: if (config.forceIntegerTicks) String.format(
                                    Locale.US,
                                    "%.0f",
                                    yVal
                                ) else String.format(Locale.US, "%.1f", yVal)
                            canvas.drawText(
                                labelText,
                                leftPadding - 6f * density,
                                y + 4f * density,
                                labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        }
                    }
                }
            } else {
                val steps = 4
                for (i in 0..steps) {
                    val y = topPadding + (drawHeight * (i.toFloat() / steps))
                    canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                    if (config.showAxisLabels) {
                        val yVal = maxVal - (valRange * (i.toFloat() / steps))
                        canvas.drawText(
                            String.format(Locale.US, "%.0f", yVal),
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }
        }

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

            // Draw wick
            canvas.drawLine(cx, highY, cx, lowY, wickPaint)

            // Draw body
            val topBody = minOf(openY, closeY)
            val bottomBody = maxOf(openY, closeY)
            val bodyHeight = (bottomBody - topBody).coerceAtLeast(2f * density)

            val left = cx - (candleWidth / 2f)
            val right = cx + (candleWidth / 2f)
            val bodyRect = RectF(left, topBody, right, topBody + bodyHeight * progress)
            canvas.drawRoundRect(bodyRect, 2f * density, 2f * density, candlePaint)

            if (config.showAxisLabels) {
                val labelText = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                canvas.drawText(
                    labelText,
                    cx,
                    baselineY + 14f * density,
                    labelPaint.apply { textAlign = Paint.Align.CENTER })
            }
        }
    }
}
