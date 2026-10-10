package com.rl04x.koracharts.core.renderer.helper

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.withRotation
import com.rl04x.koracharts.core.engine.AxisTickCalculator
import com.rl04x.koracharts.core.engine.TickResult
import com.rl04x.koracharts.core.model.ChartConfig
import java.util.Locale

/**
 * Reusable helper renderer for drawing horizontal/vertical grid lines and axis labels
 * with top headroom support without allocating objects during canvas draw operations.
 */
public class AxisGridRenderer {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    /**
     * Draws horizontal grid lines and Y-axis labels using calculated nice ticks or linear steps.
     *
     * @return Calculated [TickResult] if nice ticks were enabled, or null otherwise.
     */
    public fun drawYAxisGridAndLabels(
        canvas: Canvas,
        config: ChartConfig,
        minVal: Float,
        maxVal: Float,
        leftPadding: Float,
        topPadding: Float,
        rightPadding: Float,
        drawWidth: Float,
        drawHeight: Float,
        density: Float,
        topHeadroom: Float = 0f,
    ): TickResult? {
        gridPaint.color = config.style.gridColor
        axisPaint.color = config.style.axisColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density

        val baselineY = topPadding + topHeadroom + drawHeight
        val width = leftPadding + drawWidth + rightPadding

        val tickResult = if (config.useNiceTicks) {
            AxisTickCalculator.computeNiceTicks(
                min = minVal,
                max = maxVal,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMin = tickResult?.niceMin ?: minVal
        val effectiveMax = tickResult?.niceMax ?: maxVal
        val effectiveRange = (effectiveMax - effectiveMin).coerceAtLeast(1f)

        if (config.showGrid) {
            if (tickResult != null) {
                for (yVal in tickResult.ticks) {
                    val pct = (yVal - effectiveMin) / effectiveRange
                    val y = topPadding + topHeadroom + (1f - pct) * drawHeight
                    if (y in (topPadding - 1f)..(baselineY + 1f)) {
                        canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                        if (config.showAxisLabels) {
                            val labelText = formatYValue(yVal, config)
                            labelPaint.textAlign = Paint.Align.RIGHT
                            canvas.drawText(
                                labelText,
                                leftPadding - 6f * density,
                                y + 4f * density,
                                labelPaint
                            )
                        }
                    }
                }
            } else {
                val steps = 4
                for (i in 0..steps) {
                    val y = topPadding + topHeadroom + (drawHeight * (i.toFloat() / steps))
                    canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                    if (config.showAxisLabels) {
                        val yVal = maxVal - ((maxVal - minVal) * (i.toFloat() / steps))
                        val labelText = formatYValue(yVal, config)
                        labelPaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(
                            labelText,
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint
                        )
                    }
                }
            }
        }

        if (config.showAxes) {
            canvas.drawLine(
                leftPadding,
                topPadding + topHeadroom,
                leftPadding,
                baselineY,
                axisPaint
            )
            canvas.drawLine(leftPadding, baselineY, width - rightPadding, baselineY, axisPaint)
        }

        return tickResult
    }

    /**
     * Draws X-axis labels with optional rotation.
     */
    public fun drawXAxisLabel(
        canvas: Canvas,
        label: String,
        cx: Float,
        baselineY: Float,
        rotationDeg: Float,
        config: ChartConfig,
        density: Float,
    ) {
        if (!config.showAxisLabels || label.isEmpty()) return

        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density

        if (rotationDeg != 0f) {
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.withRotation(rotationDeg, cx, baselineY + 16f * density) {
                canvas.drawText(label, cx, baselineY + 16f * density, labelPaint)
            }
        } else {
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, cx, baselineY + 16f * density, labelPaint)
        }
    }

    private fun formatYValue(value: Float, config: ChartConfig): String {
        config.yAxisFormatter?.let { return it.invoke(value) }
        if (config.compactNumberFormatting) {
            return com.rl04x.koracharts.core.util.NumberFormatterUtils.formatCompact(
                value,
                config.valuePrefix,
                config.valueSuffix
            )
        }
        val numStr = if (config.forceIntegerTicks) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return "${config.valuePrefix}$numStr${config.valueSuffix}"
    }
}
