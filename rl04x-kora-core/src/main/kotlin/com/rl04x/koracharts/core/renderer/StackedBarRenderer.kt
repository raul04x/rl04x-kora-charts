package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.StackedBarEntry
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import java.util.Locale

/**
 * Renderer for Stacked Bar Charts supporting top-only rounded corners, segment value labels,
 * multi-row word-wrap legend bar, 2D Zoom & Pan gestures, crosshairs, and Glassmorphic badge tooltips with multi-line (\n) support.
 */
public class StackedBarRenderer : BaseRenderer<StackedBarEntry> {

    private val axisGridRenderer = AxisGridRenderer()
    private val legendRenderer = LegendRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val segmentValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val highlightBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val clipPath = Path()
    private val cachedRect = RectF()

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
        segmentValuePaint.textSize = 9f * density
        totalValuePaint.color = config.style.titleTextColor
        totalValuePaint.textSize = 10f * density
        highlightBorderPaint.color = config.style.highlightLineColor

        val firstEntry = data.first()
        val hasLegend = config.showLegend && firstEntry.colors.isNotEmpty()

        val legendLabels = if (hasLegend) {
            firstEntry.colors.indices.map { cIdx ->
                firstEntry.segmentLabels?.getOrNull(cIdx) ?: "Series ${cIdx + 1}"
            }
        } else emptyList()

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding = config.paddingDp * density + 10f * density
        val drawWidth = width - leftPadding - rightPadding

        val legendHeight = if (hasLegend) legendRenderer.calculateLegendHeight(
            legendLabels,
            drawWidth,
            density
        ) else 0f
        val hasRotatedLabels =
            config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.any {
                (it.label?.length ?: 0) > 8
            })

        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding = config.paddingDp * density + legendHeight + 6f * density
        val topHeadroom = 10f * density
        val drawHeight = height - topPadding - bottomPadding - topHeadroom
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + topHeadroom + drawHeight
        val totals = data.map { it.values.sum() }
        val maxTotal = totals.maxOrNull()?.coerceAtLeast(1f) ?: 100f

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) maxTotal else maxTotal / visibleZoomY
        val maxPanY = (maxTotal - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMaxY = clampedPanY + visibleRangeY

        val tickResult = axisGridRenderer.drawYAxisGridAndLabels(
            canvas = canvas,
            config = config,
            minVal = clampedPanY,
            maxVal = visibleMaxY,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            density = density,
            topHeadroom = topHeadroom,
        )

        val effectiveMinY = tickResult?.niceMin ?: clampedPanY
        val effectiveMaxY = tickResult?.niceMax ?: visibleMaxY
        val effectiveValRange = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        if (hasLegend) {
            legendRenderer.drawLegend(
                canvas = canvas,
                labels = legendLabels,
                colors = firstEntry.colors,
                startX = leftPadding,
                startY = config.paddingDp * density + 10f * density,
                maxLegendWidth = drawWidth,
                config = config,
                density = density,
            )
        }

        val count = data.size
        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val scaledSlotWidth = (drawWidth * effectiveZoomX) / count
        val barWidth = scaledSlotWidth * 0.55f

        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        var selectedTooltipText: String? = null
        var selectedTooltipX = 0f
        var selectedTooltipY = 0f

        canvas.withClip(
            leftPadding - 4f * density,
            topPadding,
            width - rightPadding + 4f * density,
            baselineY
        ) {
            for ((idx, entry) in data.withIndex()) {
                val cx =
                    leftPadding + (idx * scaledSlotWidth) + (scaledSlotWidth / 2f) - clampedPanPixels
                val barLeft = cx - (barWidth / 2f)
                val barRight = cx + (barWidth / 2f)

                val entryTotal = entry.values.sum()
                val totalPct = ((entryTotal - effectiveMinY) / effectiveValRange).coerceAtLeast(0f)
                val barHeight = totalPct * drawHeight * progress
                val totalTopY =
                    (baselineY - barHeight).coerceIn(topPadding + topHeadroom, baselineY)

                var currentBottomY = baselineY
                val cornerRadius = 6f * density

                clipPath.reset()
                cachedRect.set(barLeft, totalTopY, barRight, baselineY)
                clipPath.addRoundRect(
                    cachedRect,
                    floatArrayOf(
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius,
                        0f, 0f, 0f, 0f
                    ),
                    Path.Direction.CW
                )

                canvas.withClip(clipPath) {
                    for ((vIdx, valItem) in entry.values.withIndex()) {
                        val color = entry.colors.getOrElse(vIdx) { 0xFF888888.toInt() }
                        val segmentPct = (valItem / effectiveValRange).coerceAtLeast(0f)
                        val segmentHeight = segmentPct * drawHeight * progress
                        val segmentTopY = currentBottomY - segmentHeight

                        segmentPaint.color = color
                        canvas.drawRect(
                            barLeft,
                            segmentTopY,
                            barRight,
                            currentBottomY,
                            segmentPaint
                        )

                        if (segmentHeight > 14f * density && config.showPointValues) {
                            val segmentText = if (valItem == valItem.toLong().toFloat()) {
                                String.format(Locale.US, "%.0f", valItem)
                            } else {
                                String.format(Locale.US, "%.1f", valItem)
                            }
                            canvas.drawText(
                                segmentText,
                                cx,
                                segmentTopY + (segmentHeight / 2f) + 3f * density,
                                segmentValuePaint
                            )
                        }

                        currentBottomY = segmentTopY
                    }
                }

                val isSelected =
                    (config.selectedEntry?.x == entry.x || config.selectedEntry?.label == entry.label)
                if (isSelected) {
                    canvas.drawRoundRect(
                        cachedRect,
                        cornerRadius,
                        cornerRadius,
                        highlightBorderPaint
                    )
                    glassTooltipRenderer.drawCrosshairs(
                        canvas = canvas,
                        x = cx,
                        y = totalTopY,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        config = config,
                    )

                    // Prepare multi-line tooltip card breakdown
                    val entryLabel = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                    val sb = StringBuilder(entryLabel)
                    for (vIdx in entry.values.indices) {
                        val valItem = entry.values[vIdx]
                        val segName = entry.segmentLabels?.getOrNull(vIdx) ?: "Series ${vIdx + 1}"
                        val formattedVal = if (valItem == valItem.toLong().toFloat()) {
                            String.format(Locale.US, "%.0f", valItem)
                        } else {
                            String.format(Locale.US, "%.1f", valItem)
                        }
                        sb.append("\n").append(segName).append(": ").append(config.valuePrefix)
                            .append(formattedVal).append(config.valueSuffix)
                    }
                    val formattedTotal = if (entryTotal == entryTotal.toLong().toFloat()) {
                        String.format(Locale.US, "%.0f", entryTotal)
                    } else {
                        String.format(Locale.US, "%.1f", entryTotal)
                    }
                    sb.append("\nTotal: ").append(config.valuePrefix).append(formattedTotal)
                        .append(config.valueSuffix)

                    selectedTooltipText = sb.toString()
                    selectedTooltipX = cx
                    selectedTooltipY = totalTopY
                }

                if (config.showPointValues && totalTopY in (topPadding + topHeadroom)..baselineY && !isSelected) {
                    val totalText = if (entryTotal == entryTotal.toLong().toFloat()) {
                        String.format(Locale.US, "%.0f", entryTotal)
                    } else {
                        String.format(Locale.US, "%.1f", entryTotal)
                    }
                    canvas.drawText(totalText, cx, totalTopY - 4f * density, totalValuePaint)
                }
            }
        }

        // Draw X axis labels
        if (config.showAxisLabels) {
            val defaultRotation = if (hasRotatedLabels) {
                if (config.xAxisLabelRotation != 0f) config.xAxisLabelRotation else -45f
            } else 0f

            for ((idx, entry) in data.withIndex()) {
                val cx =
                    leftPadding + (idx * scaledSlotWidth) + (scaledSlotWidth / 2f) - clampedPanPixels
                if (cx in (leftPadding - 8f * density)..(width - rightPadding + 8f * density)) {
                    val label = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                    axisGridRenderer.drawXAxisLabel(
                        canvas = canvas,
                        label = label,
                        cx = cx,
                        baselineY = baselineY,
                        rotationDeg = defaultRotation,
                        config = config,
                        density = density,
                    )
                }
            }
        }

        // Draw selected tooltip badge on top of everything
        selectedTooltipText?.let { tooltipText ->
            glassTooltipRenderer.drawGlassBadge(
                canvas = canvas,
                text = tooltipText,
                cx = selectedTooltipX,
                cy = selectedTooltipY,
                canvasWidth = width,
                config = config,
                density = density,
                isSelected = true,
            )
        }
    }
}
