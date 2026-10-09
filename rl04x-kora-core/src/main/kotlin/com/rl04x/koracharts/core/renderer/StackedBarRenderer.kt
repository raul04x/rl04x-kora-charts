package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withRotation
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.StackedBarEntry
import java.util.Locale

/**
 * Renderer for Stacked Bar Charts with top-only rounded corners and segment value labels.
 */
public class StackedBarRenderer : BaseRenderer<StackedBarEntry> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val segmentValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    private val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<StackedBarEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        gridPaint.color = config.style.gridColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density
        segmentValuePaint.textSize = 9f * density
        totalValuePaint.color = config.style.titleTextColor
        totalValuePaint.textSize = 10f * density

        val hasRotatedLabels =
            config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.any {
                (it.label?.length ?: 0) > 8
            })

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 24f * density else config.paddingDp * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding = config.paddingDp * density + 20f * density
        val rightPadding = config.paddingDp * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight
        val totals = data.map { it.values.sum() }
        val maxTotal = totals.maxOrNull()?.coerceAtLeast(1f) ?: 100f

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = 0f,
                max = maxTotal,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
            )
        } else null

        val effectiveMaxTotal = yTickResult?.niceMax ?: maxTotal

        // Draw horizontal grid lines
        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val pct = yVal / effectiveMaxTotal
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
                        val valY = maxTotal * (1f - (i.toFloat() / steps))
                        canvas.drawText(
                            String.format(Locale.US, "%.0f", valY),
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }
        }

        val entryCount = data.size
        val slotWidth = drawWidth / entryCount
        val barWidth = slotWidth * 0.52f

        for ((eIdx, entry) in data.withIndex()) {
            val slotCenterX = leftPadding + (eIdx * slotWidth) + (slotWidth / 2f)
            val barLeft = slotCenterX - (barWidth / 2f)
            val barRight = slotCenterX + (barWidth / 2f)

            var currentY = baselineY
            for ((vIdx, valItem) in entry.values.withIndex()) {
                val segmentHeight = (valItem / effectiveMaxTotal) * drawHeight * progress
                val segmentTop = currentY - segmentHeight

                segmentPaint.color =
                    entry.colors.getOrElse(vIdx) { config.style.highlightLineColor }
                val rect = RectF(barLeft, segmentTop, barRight, currentY)

                val isTopSegment = (vIdx == entry.values.lastIndex)
                if (isTopSegment) {
                    val r = 6f * density
                    val path = Path().apply {
                        addRoundRect(
                            rect,
                            floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f),
                            Path.Direction.CW,
                        )
                    }
                    canvas.drawPath(path, segmentPaint)
                } else {
                    canvas.drawRect(rect, segmentPaint)
                }

                // Draw segment value label inside segment if there is enough height
                if (segmentHeight >= 14f * density) {
                    val segmentColor =
                        entry.colors.getOrElse(vIdx) { config.style.highlightLineColor }
                    val customTextColor = entry.textColors?.getOrNull(vIdx)

                    val contrastTextColor =
                        customTextColor
                            ?: com.rl04x.koracharts.core.util.NumberFormatterUtils.calculateHarmoniousContrastColor(
                                segmentColor
                            )

                    segmentValuePaint.color = contrastTextColor

                    val valText = String.format(Locale.US, "%.0f", valItem)
                    canvas.drawText(
                        valText,
                        slotCenterX,
                        segmentTop + (segmentHeight / 2f) + 3f * density,
                        segmentValuePaint
                    )
                }

                currentY = segmentTop
            }

            // Draw total value label on top of the bar
            val totalVal = entry.values.sum()
            val animatedTotalY =
                baselineY - ((totalVal / effectiveMaxTotal) * drawHeight * progress)
            canvas.drawText(
                String.format(Locale.US, "%.0f", totalVal),
                slotCenterX,
                animatedTotalY - 6f * density,
                totalValuePaint,
            )
        }

        // Draw X axis labels in unclipped space below baseline
        if (config.showAxisLabels) {
            for ((eIdx, entry) in data.withIndex()) {
                val slotCenterX = leftPadding + (eIdx * slotWidth) + (slotWidth / 2f)
                if (slotCenterX in leftPadding..width - rightPadding) {
                    val rawText = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                    val rotation = when {
                        config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                        rawText.length > 8 -> -90f
                        else -> 0f
                    }

                    val labelY = baselineY + 10f * density

                    if (rotation != 0f) {
                        canvas.withRotation(rotation, slotCenterX, labelY) {
                            drawText(
                                rawText,
                                slotCenterX,
                                labelY,
                                labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        }
                    } else {
                        val labelText =
                            if (rawText.length > config.xAxisLabelMaxLen) rawText.take(config.xAxisLabelMaxLen - 1) + "…" else rawText
                        canvas.drawText(
                            labelText,
                            slotCenterX,
                            labelY,
                            labelPaint.apply { textAlign = Paint.Align.CENTER })
                    }
                }
            }
        }
    }
}
