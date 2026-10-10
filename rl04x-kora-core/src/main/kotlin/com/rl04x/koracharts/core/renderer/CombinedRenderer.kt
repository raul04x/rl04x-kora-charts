package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale

/**
 * Renderer for Combined Bar + Line Overlay Charts.
 */
public class CombinedRenderer : BaseRenderer<Dataset> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<Dataset>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.size < 2 || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        gridPaint.color = config.style.gridColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 24f * density else config.paddingDp * density
        val bottomPadding =
            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
        val topPadding = config.paddingDp * density + 26f * density
        val rightPadding = config.paddingDp * density + 10f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight

        val barDataset = data[0]
        val lineDataset = data[1]
        val entryCount = maxOf(barDataset.entries.size, lineDataset.entries.size)
        if (entryCount == 0) return

        val maxVal = maxOf(
            barDataset.entries.maxOfOrNull { it.y } ?: 100f,
            lineDataset.entries.maxOfOrNull { it.y } ?: 100f,
        ).coerceAtLeast(1f)

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = 0f,
                max = maxVal,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMaxVal = yTickResult?.niceMax ?: maxVal

        // Draw horizontal grid lines
        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val pct = yVal / effectiveMaxVal
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
                        val yVal = maxVal * (1f - (i.toFloat() / steps))
                        canvas.drawText(
                            String.format(Locale.US, "%.0f", yVal),
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }
        }

        val slotWidth = drawWidth / entryCount
        val barWidth = slotWidth * 0.45f

        // 1. Draw bars
        barPaint.color = barDataset.color
        for ((idx, entry) in barDataset.entries.withIndex()) {
            val cx = leftPadding + (idx * slotWidth) + (slotWidth / 2f)
            val barLeft = cx - (barWidth / 2f)
            val barRight = cx + (barWidth / 2f)
            val barHeight = (entry.y / effectiveMaxVal) * drawHeight * progress
            val barTop = baselineY - barHeight

            canvas.drawRoundRect(
                barLeft,
                barTop,
                barRight,
                baselineY,
                6f * density,
                6f * density,
                barPaint
            )

            if (config.showAxisLabels) {
                val labelText = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                canvas.drawText(
                    labelText,
                    cx,
                    baselineY + 14f * density,
                    labelPaint.apply { textAlign = Paint.Align.CENTER })
            }
        }

        // 2. Draw overlay line curve
        linePaint.color = lineDataset.color
        linePaint.strokeWidth = lineDataset.lineWidth * density
        nodePaint.color = lineDataset.color

        val linePath = Path()
        val linePoints = mutableListOf<Pair<Float, Float>>()

        for ((idx, entry) in lineDataset.entries.withIndex()) {
            val cx = leftPadding + (idx * slotWidth) + (slotWidth / 2f)
            val lineY = baselineY - ((entry.y / effectiveMaxVal) * drawHeight * progress)
            linePoints.add(Pair(cx, lineY))

            if (idx == 0) linePath.moveTo(cx, lineY) else linePath.lineTo(cx, lineY)
        }

        canvas.drawPath(linePath, linePaint)

        // Draw node dots
        for (pt in linePoints) {
            canvas.drawCircle(pt.first, pt.second, 5f * density, nodePaint)
            canvas.drawCircle(
                pt.first,
                pt.second,
                2.5f * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.style.cardBackgroundColor })
        }
    }
}
