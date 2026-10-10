package com.rl04x.koracharts.core.renderer.helper

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.withRotation
import com.rl04x.koracharts.core.engine.AxisTickCalculator
import com.rl04x.koracharts.core.engine.TickResult
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Entry
import java.util.Locale

/**
 * Reusable helper renderer for drawing horizontal/vertical grid lines and axis labels
 * with Y & X axis nice tick calculations and top headroom support.
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
     * Draws vertical grid lines and X-axis labels using calculated nice ticks or category entry labels.
     */
    public fun drawXAxisGridAndLabels(
        canvas: Canvas,
        config: ChartConfig,
        entries: List<Entry>,
        minX: Float,
        maxX: Float,
        leftPadding: Float,
        topPadding: Float,
        drawWidth: Float,
        drawHeight: Float,
        density: Float,
        topHeadroom: Float = 0f,
        effectiveZoomX: Float = 1f,
        clampedPanPixels: Float = 0f,
        defaultRotation: Float = 0f,
    ) {
        if (!config.showAxisLabels && !config.showVerticalGrid) return

        gridPaint.color = config.style.gridColor
        val baselineY = topPadding + topHeadroom + drawHeight
        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val hasCategoryLabels = entries.any { !it.label.isNullOrEmpty() }

        if (config.useNiceTicks && !hasCategoryLabels && rangeX > 1f) {
            val xTickResult = AxisTickCalculator.computeNiceTicks(
                min = minX,
                max = maxX,
                targetTicks = 4,
                customStep = config.xAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = false,
            )

            for (xVal in xTickResult.ticks) {
                val pctX = (xVal - minX) / rangeX
                val cx = leftPadding + (pctX * drawWidth * effectiveZoomX) - clampedPanPixels

                if (cx in (leftPadding - 8f * density)..(leftPadding + drawWidth + 8f * density)) {
                    if (config.showVerticalGrid) {
                        canvas.drawLine(cx, topPadding + topHeadroom, cx, baselineY, gridPaint)
                    }
                    if (config.showAxisLabels) {
                        val rawText = config.xAxisFormatter?.invoke(xVal)
                            ?: if (config.forceIntegerTicks) String.format(
                                Locale.US,
                                "%.0f",
                                xVal
                            ) else String.format(Locale.US, "%.1f", xVal)
                        drawXAxisLabel(
                            canvas,
                            rawText,
                            cx,
                            baselineY,
                            defaultRotation,
                            config,
                            density
                        )
                    }
                }
            }
        } else {
            val entryCount = entries.size
            if (entryCount == 0) return

            labelPaint.textSize = 10f * density
            var sampleStride = 1
            if (entryCount > 1 && defaultRotation == 0f) {
                val rawSampleText = config.xAxisFormatter?.invoke(entries.first().x)
                    ?: entries.first().label?.substringBefore('\n')
                    ?: "Day 00"
                val sampleText = if (rawSampleText.length > config.xAxisLabelMaxLen) {
                    rawSampleText.take(config.xAxisLabelMaxLen)
                } else rawSampleText

                val textWidth = labelPaint.measureText(sampleText) + 14f * density
                val maxVisibleLabels =
                    ((drawWidth * effectiveZoomX) / textWidth).toInt().coerceAtLeast(1)
                sampleStride = (entryCount / maxVisibleLabels).coerceAtLeast(1)
            }

            val startPos = if (sampleStride > 1) sampleStride else 1
            for (pos in startPos..entryCount step sampleStride) {
                val idx = pos - 1
                val entry = entries[idx]
                val pctX = if (entryCount > 1) {
                    if (rangeX > 0f) (entry.x - minX) / rangeX else idx.toFloat() / (entryCount - 1)
                } else 0.5f

                val cx = leftPadding + (pctX * drawWidth * effectiveZoomX) - clampedPanPixels

                if (cx in (leftPadding - 8f * density)..(leftPadding + drawWidth + 8f * density)) {
                    if (config.showVerticalGrid) {
                        canvas.drawLine(cx, topPadding + topHeadroom, cx, baselineY, gridPaint)
                    }
                    if (config.showAxisLabels) {
                        val rawText = config.xAxisFormatter?.invoke(entry.x)
                            ?: entry.label?.substringBefore('\n')
                            ?: String.format(Locale.US, "%.0f", entry.x)

                        val labelText =
                            if (rawText.length > config.xAxisLabelMaxLen && defaultRotation == 0f) {
                                rawText.take(config.xAxisLabelMaxLen - 1) + "…"
                            } else rawText

                        drawXAxisLabel(
                            canvas,
                            labelText,
                            cx,
                            baselineY,
                            defaultRotation,
                            config,
                            density
                        )
                    }
                }
            }
        }
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
