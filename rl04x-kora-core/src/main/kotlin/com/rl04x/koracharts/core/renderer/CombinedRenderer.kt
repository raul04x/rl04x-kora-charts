package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import java.util.Locale

/**
 * Renderer for Combined Bar + Line Overlay Charts supporting smooth curves, offscreen path clipping, 2D Zoom & Pan gestures, crosshairs, Glass tooltips, and zero allocations in draw().
 */
public class CombinedRenderer : BaseRenderer<Dataset> {

    private val axisGridRenderer = AxisGridRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()
    private val legendRenderer = LegendRenderer()

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

        val barDataset = data[0]
        val lineDataset = data[1]
        val entryCount = maxOf(barDataset.entries.size, lineDataset.entries.size)
        if (entryCount == 0) return

        val hasRotatedLabels = config.showAxisLabels && (
                config.xAxisLabelRotation != 0f || barDataset.entries.any {
                    (it.label?.substringBefore('\n')?.length ?: 0) > 8
                }
                )
        val defaultRotation = if (hasRotatedLabels) {
            if (config.xAxisLabelRotation != 0f) config.xAxisLabelRotation else -90f
        } else 0f

        val hasSecondaryY = config.showSecondaryYAxis || data.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && data.any { it.label.isNotEmpty() }

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density + 18f * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 16f * density)
        } else {
            config.paddingDp * density
        }

        val legendLabels = if (hasLegend) data.filter { it.visible && it.label.isNotEmpty() }
            .map { it.label } else emptyList()
        val legendColors = if (hasLegend) data.filter { it.visible && it.label.isNotEmpty() }
            .map { it.color } else emptyList()
        val legendHeight = if (hasLegend) legendRenderer.calculateLegendHeight(
            legendLabels,
            width - leftPadding - rightPadding,
            density
        ) else 0f

        val topPadding = config.paddingDp * density + legendHeight + 6f * density
        val topHeadroom = 10f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding - topHeadroom
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val engine = ChartEngine(width, height, config.paddingDp * density)
        val range = engine.computeRange(data)

        val minDataY = minOf(0f, range.minY)
        val maxDataY = maxOf(1f, range.maxY)
        val fullRangeY = maxDataY - minDataY

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) fullRangeY else fullRangeY / visibleZoomY
        val maxPanY = (fullRangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)

        val visibleMinY = minDataY + clampedPanY
        val visibleMaxY = visibleMinY + visibleRangeY

        val tickResult = axisGridRenderer.drawYAxisGridAndLabels(
            canvas = canvas,
            config = config.copy(capNiceTicksAtMax = false),
            minVal = visibleMinY,
            maxVal = visibleMaxY,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            density = density,
            topHeadroom = topHeadroom,
        )

        val effectiveMinY = visibleMinY
        val effectiveMaxY = tickResult?.niceMax ?: visibleMaxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)
        val baselineY = topPadding + topHeadroom + drawHeight

        if (hasLegend) {
            legendRenderer.drawLegend(
                canvas = canvas,
                labels = legendLabels,
                colors = legendColors,
                startX = leftPadding,
                startY = config.paddingDp * density + 10f * density,
                maxLegendWidth = drawWidth,
                config = config,
                density = density,
            )
        }

        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
        val barWidth = scaledSlotWidth * 0.45f

        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        var selectedBadgeText: String? = null
        var selectedBadgeX = 0f
        var selectedBadgeY = 0f

        // PASS 1: Draw Bars and Line Overlay clipped to plotting area
        canvas.withClip(
            leftPadding,
            topPadding,
            width - rightPadding + 4f * density,
            baselineY
        ) {
            // 1. Draw Bars
            barPaint.color = barDataset.color
            for ((idx, entry) in barDataset.entries.withIndex()) {
                val cx =
                    leftPadding + (idx * scaledSlotWidth) + (scaledSlotWidth / 2f) - clampedPanPixels
                val barLeft = cx - (barWidth / 2f)
                val barRight = cx + (barWidth / 2f)

                val pctY = ((entry.y - effectiveMinY) / effectiveRangeY).coerceAtLeast(0f)
                val targetY = topPadding + topHeadroom + (1f - pctY) * drawHeight
                val barTop = (baselineY - ((baselineY - targetY) * progress)).coerceIn(
                    topPadding + topHeadroom,
                    baselineY
                )

                cachedBarRect.set(barLeft, barTop, barRight, baselineY)
                canvas.drawRoundRect(cachedBarRect, 4f * density, 4f * density, barPaint)

                val isSelected = (config.selectedEntry == entry)
                if (isSelected) {
                    val clampedBarTop = barTop.coerceIn(topPadding + topHeadroom, baselineY)
                    glassTooltipRenderer.drawCrosshairs(
                        canvas = canvas,
                        x = cx,
                        y = clampedBarTop,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        config = config,
                    )

                    selectedBadgeX = cx
                    selectedBadgeY = clampedBarTop - 12f * density
                    val labelText = entry.label ?: String.format(Locale.US, "Day %.0f", entry.x)
                    selectedBadgeText = "$labelText\nUsers: ${entry.y.toInt()}"
                }
            }

            // 2. Draw Line Overlay
            linePaint.color = lineDataset.color
            linePaint.strokeWidth = lineDataset.lineWidth * density
            nodePaint.color = lineDataset.color
            innerNodePaint.color = config.style.cardBackgroundColor

            linePath.reset()
            val linePoints = FloatArray(lineDataset.entries.size * 2)
            var validCount = 0

            var prevX = 0f
            var prevY = 0f

            for ((idx, entry) in lineDataset.entries.withIndex()) {
                val cx =
                    leftPadding + (idx * scaledSlotWidth) + (scaledSlotWidth / 2f) - clampedPanPixels

                val pctY = ((entry.y - effectiveMinY) / effectiveRangeY).coerceAtLeast(0f)
                val targetY = topPadding + topHeadroom + (1f - pctY) * drawHeight
                val lineY = baselineY - ((baselineY - targetY) * progress)

                linePoints[idx * 2] = cx
                linePoints[idx * 2 + 1] = lineY
                validCount++

                if (idx == 0) {
                    linePath.moveTo(cx, lineY)
                } else {
                    if (lineDataset.isCurved) {
                        val controlX1 = prevX + (cx - prevX) / 2f
                        val controlX2 = prevX + (cx - prevX) / 2f
                        linePath.cubicTo(
                            controlX1,
                            prevY,
                            controlX2,
                            lineY,
                            cx,
                            lineY
                        )
                    } else {
                        linePath.lineTo(cx, lineY)
                    }
                }

                prevX = cx
                prevY = lineY
            }

            canvas.drawPath(linePath, linePaint)

            // Draw Node Dots only if on screen
            for (idx in 0 until validCount) {
                val px = linePoints[idx * 2]
                val py = linePoints[idx * 2 + 1]
                val entry = lineDataset.entries[idx]

                if (py in (topPadding + topHeadroom - 4f * density)..(baselineY + 4f * density)) {
                    canvas.drawCircle(px, py, 4.5f * density, nodePaint)
                    canvas.drawCircle(px, py, 2f * density, innerNodePaint)
                }

                val isSelected = (config.selectedEntry == entry)
                if (isSelected) {
                    val clampedPy = py.coerceIn(topPadding + topHeadroom, baselineY)
                    glassTooltipRenderer.drawHighlightPoint(
                        canvas = canvas,
                        x = px,
                        y = py,
                        pointColor = lineDataset.color,
                        density = density,
                    )
                    glassTooltipRenderer.drawCrosshairs(
                        canvas = canvas,
                        x = px,
                        y = py,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        config = config,
                    )

                    selectedBadgeX = px
                    selectedBadgeY = clampedPy - 12f * density
                    val labelText = entry.label ?: String.format(Locale.US, "Day %.0f", entry.x)
                    selectedBadgeText = "$labelText\nConversions: ${entry.y.toInt()}"
                }
            }
        }

        // PASS 2: Draw X axis labels aligned to slot centers
        if (config.showAxisLabels || config.showVerticalGrid) {
            axisGridRenderer.drawXAxisGridAndLabels(
                canvas = canvas,
                config = config,
                entries = barDataset.entries,
                minX = range.minX,
                maxX = range.maxX,
                leftPadding = leftPadding,
                topPadding = topPadding,
                drawWidth = drawWidth,
                drawHeight = drawHeight,
                density = density,
                topHeadroom = topHeadroom,
                effectiveZoomX = effectiveZoomX,
                clampedPanPixels = clampedPanPixels,
                defaultRotation = defaultRotation,
                useSlotCenters = true,
            )
        }

        // PASS 3: Draw selected tooltip badge
        selectedBadgeText?.let { tooltipText ->
            glassTooltipRenderer.drawGlassBadge(
                canvas = canvas,
                text = tooltipText,
                cx = selectedBadgeX,
                cy = selectedBadgeY,
                canvasWidth = width,
                config = config,
                density = density,
                isSelected = true,
            )
        }
    }
}
