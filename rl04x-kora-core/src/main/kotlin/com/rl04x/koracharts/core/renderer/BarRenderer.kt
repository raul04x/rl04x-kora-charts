package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
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
 * Renderer for Bar Charts with top-only rounded corners (flat base), Glassmorphic badge overlays,
 * top headroom for capped axes, and Y=0 anchored baselines.
 */
public class BarRenderer : BaseRenderer<Dataset> {

    private val axisGridRenderer = AxisGridRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()
    private val legendRenderer = LegendRenderer()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    // Cached RectF and Path instances to prevent allocation during draw()
    private val cachedBarRect = RectF()
    private val barPath = Path()
    private val cachedZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cachedRefLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cachedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Reusable buffers for pending badges to avoid list recreation on every frame
    private class PendingBadge(
        var text: String = "",
        var x: Float = 0f,
        var y: Float = 0f,
        var isSelected: Boolean = false,
    )

    private val badgePool = ArrayList<PendingBadge>(32).apply {
        repeat(32) { add(PendingBadge()) }
    }
    private var badgeCount = 0

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<Dataset>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        badgeCount = 0

        val visibleDatasets = data.filter { it.visible && it.entries.isNotEmpty() }
        if (visibleDatasets.isEmpty()) return

        val firstDataset = visibleDatasets.first()
        val entryCount = firstDataset.entries.size

        val hasRotatedLabels = config.showAxisLabels && (
                config.xAxisLabelRotation != 0f ||
                        firstDataset.entries.any { e ->
                            val text = config.xAxisFormatter?.invoke(e.x)
                                ?: e.label?.substringBefore('\n') ?: ""
                            text.length > 5 || (entryCount > 5 && text.length > 4)
                        }
                )

        val defaultRotation = if (hasRotatedLabels) {
            if (config.xAxisLabelRotation != 0f) config.xAxisLabelRotation else -45f
        } else 0f

        val hasSecondaryY = config.showSecondaryYAxis || data.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && data.any { it.label.isNotEmpty() }

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density
            else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density
            else config.paddingDp * density + 10f * density
        val bottomPadding =
            if (config.showAxisLabels) {
                config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
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

        // Bar Charts MUST start Y at 0 (or minOf(0f, minY) if negative values exist)
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
            config = config,
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

        val effectiveMinY = tickResult?.niceMin ?: visibleMinY
        val effectiveMaxY = tickResult?.niceMax ?: visibleMaxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)
        val baselineY = topPadding + topHeadroom + drawHeight

        // Draw Target Zones
        for ((minY, maxY, label, color, fillAlpha) in config.targetZones) {
            val topPct = ((maxY - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val bottomPct = ((minY - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val zTop = topPadding + topHeadroom + (1f - topPct) * drawHeight
            val zBottom = topPadding + topHeadroom + (1f - bottomPct) * drawHeight

            cachedZonePaint.color = color
            cachedZonePaint.alpha = (fillAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(leftPadding, zTop, width - rightPadding, zBottom, cachedZonePaint)

            label?.let { label ->
                cachedTextPaint.color = color
                cachedTextPaint.textSize = 9f * density
                cachedTextPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    label,
                    leftPadding + 6f * density,
                    zTop + 12f * density,
                    cachedTextPaint
                )
            }
        }

        // Draw Reference Lines
        for ((value, label, color, isDashed, strokeWidthDp) in config.referenceLines) {
            val pct = ((value - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val refY = topPadding + topHeadroom + (1f - pct) * drawHeight
            if (refY in (topPadding + topHeadroom)..baselineY) {
                cachedRefLinePaint.color = color
                cachedRefLinePaint.strokeWidth = strokeWidthDp * density
                if (isDashed) {
                    cachedRefLinePaint.pathEffect =
                        android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                } else {
                    cachedRefLinePaint.pathEffect = null
                }
                canvas.drawLine(leftPadding, refY, width - rightPadding, refY, cachedRefLinePaint)

                label?.let { label ->
                    cachedTextPaint.color = color
                    cachedTextPaint.textSize = 9f * density
                    cachedTextPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(
                        label,
                        width - rightPadding - 6f * density,
                        refY - 4f * density,
                        cachedTextPaint
                    )
                }
            }
        }

        // Draw Legend
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
        val barWidth = scaledSlotWidth * 0.6f / visibleDatasets.size

        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        val cornerRadius = 4f * density

        // PASS 1: Drawing bars clipped strictly to plotting area
        canvas.withClip(
            leftPadding,
            topPadding,
            width - rightPadding,
            baselineY
        ) {
            for (eIdx in 0 until entryCount) {
                val slotLeft = leftPadding + (eIdx * scaledSlotWidth) - clampedPanPixels

                for ((dIdx, dataset) in visibleDatasets.withIndex()) {
                    if (eIdx >= dataset.entries.size) continue
                    val entry = dataset.entries[eIdx]
                    val barLeft =
                        slotLeft + ((scaledSlotWidth - (barWidth * visibleDatasets.size)) / 2f) + (dIdx * barWidth)
                    val barRight = barLeft + barWidth

                    val yRatio = ((entry.y - effectiveMinY) / effectiveRangeY).coerceAtLeast(0f)
                    val barHeight = yRatio * drawHeight * progress
                    val barTop =
                        (baselineY - barHeight).coerceIn(topPadding + topHeadroom, baselineY)

                    barPaint.color = dataset.color
                    cachedBarRect.set(barLeft, barTop, barRight, baselineY)

                    barPath.reset()
                    barPath.addRoundRect(
                        cachedBarRect,
                        floatArrayOf(
                            cornerRadius, cornerRadius, // Top-Left
                            cornerRadius, cornerRadius, // Top-Right
                            0f, 0f,                     // Bottom-Right (FLAT BASE)
                            0f, 0f                      // Bottom-Left  (FLAT BASE)
                        ),
                        Path.Direction.CW
                    )

                    canvas.drawPath(barPath, barPaint)

                    val isSelected = (config.selectedEntry == entry)
                    if (isSelected) {
                        highlightBorderPaint.color = config.style.highlightLineColor
                        canvas.drawPath(barPath, highlightBorderPaint)

                        val cx = barLeft + (barWidth / 2f)
                        glassTooltipRenderer.drawCrosshairs(
                            canvas = canvas,
                            x = cx,
                            y = barTop,
                            leftPadding = leftPadding,
                            topPadding = topPadding,
                            drawWidth = drawWidth,
                            drawHeight = drawHeight,
                            config = config,
                        )
                    }

                    if ((config.showPointValues || isSelected) && progress >= 0.5f) {
                        val valueText = config.pointValueFormatter?.invoke(entry)
                            ?: entry.label
                            ?: formatValue(entry.y, config)

                        val cx = barLeft + (barWidth / 2f)
                        val cy = barTop - 22f * density

                        if (badgeCount < badgePool.size) {
                            val badge = badgePool[badgeCount]
                            badge.text = valueText
                            badge.x = cx
                            badge.y = cy
                            badge.isSelected = isSelected
                        } else {
                            badgePool.add(PendingBadge(valueText, cx, cy, isSelected))
                        }
                        badgeCount++
                    }
                }
            }
        }

        // PASS 2: Drawing X axis labels
        axisGridRenderer.drawXAxisGridAndLabels(
            canvas = canvas,
            config = config,
            entries = firstDataset.entries,
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
        )

        // PASS 3: Draw Badges using GlassTooltipRenderer
        for (i in 0 until badgeCount) {
            val badge = badgePool[i]
            glassTooltipRenderer.drawGlassBadge(
                canvas = canvas,
                text = badge.text,
                cx = badge.x,
                cy = badge.y,
                canvasWidth = width,
                config = config,
                density = density,
                isSelected = badge.isSelected,
            )
        }
    }

    private fun formatValue(value: Float, config: ChartConfig): String {
        config.yAxisFormatter?.let { return it.invoke(value) }
        if (config.compactNumberFormatting) {
            return com.rl04x.koracharts.core.util.NumberFormatterUtils.formatCompact(
                value,
                config.valuePrefix,
                config.valueSuffix
            )
        }
        val numStr = if (config.forceIntegerTicks) String.format(
            Locale.US,
            "%.0f",
            value
        ) else String.format(Locale.US, "%.1f", value)
        return "${config.valuePrefix}$numStr${config.valueSuffix}"
    }
}
