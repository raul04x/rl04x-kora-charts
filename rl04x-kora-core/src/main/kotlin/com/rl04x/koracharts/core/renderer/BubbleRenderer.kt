package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.ChartConfig
import java.util.Locale

/**
 * Renderer for Bubble Scatter Plot charts.
 */
public class BubbleRenderer : BaseRenderer<BubbleEntry> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val bubbleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<BubbleEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        gridPaint.color = config.style.gridColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 24f * density else config.paddingDp * density
        val bottomPadding =
            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
        val topPadding = config.paddingDp * density + 28f * density
        val rightPadding = config.paddingDp * density + 16f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight
        val minX = data.minOf { it.x }
        val maxX = data.maxOf { it.x }
        val rangeX = (maxX - minX).coerceAtLeast(1f)

        val minY = data.minOf { it.y }
        val maxY = data.maxOf { it.y }
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = minY,
                max = maxY,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMinY = yTickResult?.niceMin ?: minY
        val effectiveMaxY = yTickResult?.niceMax ?: maxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        // Draw horizontal grid lines
        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val pct = (yVal - effectiveMinY) / effectiveRangeY
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
                        val valY = maxY - (rangeY * (i.toFloat() / steps))
                        canvas.drawText(
                            String.format(Locale.US, "%.0f", valY),
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }
        }

        // Draw translucent bubbles
        for (entry in data) {
            val cx = leftPadding + ((entry.x - minX) / rangeX) * drawWidth
            val cy = baselineY - ((entry.y - effectiveMinY) / effectiveRangeY) * drawHeight
            val radius = entry.radiusDp * density * progress

            val baseColor = entry.color
            val alphaColor =
                Color.argb(140, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            bubbleFillPaint.color = alphaColor
            canvas.drawCircle(cx, cy, radius, bubbleFillPaint)

            bubbleStrokePaint.color = baseColor
            canvas.drawCircle(cx, cy, radius, bubbleStrokePaint)

            if (config.showAxisLabels && entry.label != null) {
                canvas.drawText(entry.label, cx, baselineY + 14f * density, labelPaint)
            }
        }
    }
}
