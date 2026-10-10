package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import java.util.Locale

/**
 * Renderer for Combined Bar + Line Overlay Charts.
 */
public class CombinedRenderer : BaseRenderer<Dataset> {

    private val axisGridRenderer = AxisGridRenderer()

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

    private val innerNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cachedBarRect = RectF()
    private val linePath = Path()

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

        val tickResult = axisGridRenderer.drawYAxisGridAndLabels(
            canvas = canvas,
            config = config,
            minVal = 0f,
            maxVal = maxVal,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            density = density,
        )

        val effectiveMaxVal = tickResult?.niceMax ?: maxVal

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

            cachedBarRect.set(barLeft, barTop, barRight, baselineY)
            canvas.drawRoundRect(cachedBarRect, 6f * density, 6f * density, barPaint)

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

        // 2. Draw overlay line curve
        linePaint.color = lineDataset.color
        linePaint.strokeWidth = lineDataset.lineWidth * density
        nodePaint.color = lineDataset.color
        innerNodePaint.color = config.style.cardBackgroundColor

        linePath.reset()
        val linePoints = FloatArray(lineDataset.entries.size * 2)

        for ((idx, entry) in lineDataset.entries.withIndex()) {
            val cx = leftPadding + (idx * slotWidth) + (slotWidth / 2f)
            val lineY = baselineY - ((entry.y / effectiveMaxVal) * drawHeight * progress)
            linePoints[idx * 2] = cx
            linePoints[idx * 2 + 1] = lineY

            if (idx == 0) linePath.moveTo(cx, lineY) else linePath.lineTo(cx, lineY)
        }

        canvas.drawPath(linePath, linePaint)

        // Draw node dots
        for (idx in lineDataset.entries.indices) {
            val px = linePoints[idx * 2]
            val py = linePoints[idx * 2 + 1]
            canvas.drawCircle(px, py, 5f * density, nodePaint)
            canvas.drawCircle(px, py, 2.5f * density, innerNodePaint)
        }
    }
}
