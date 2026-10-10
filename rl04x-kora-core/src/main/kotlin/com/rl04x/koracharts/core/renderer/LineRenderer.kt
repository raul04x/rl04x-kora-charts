package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import java.util.Locale

/**
 * Renderer for Line Charts with Glassmorphic badge overlays, top headroom for capped axes, strict Y-axis clipping, and zero-allocation drawing loops.
 */
public class LineRenderer : BaseRenderer<Dataset> {

    private val axisGridRenderer = AxisGridRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()
    private val legendRenderer = LegendRenderer()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Reusable Path objects to prevent allocations in draw()
    private val linePath = Path()
    private val fillPath = Path()

    // Reusable badge buffers
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
                        firstDataset.entries.any {
                            (it.label?.substringBefore('\n')?.length ?: 0) > 8
                        }
                )
        val defaultRotation = if (hasRotatedLabels) {
            if (config.xAxisLabelRotation != 0f) config.xAxisLabelRotation else -45f
        } else 0f

        val hasSecondaryY = config.showSecondaryYAxis || data.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && data.any { it.label.isNotEmpty() }

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density + 18f * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 50f * density else 16f * density)
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

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) range.rangeY else range.rangeY / visibleZoomY
        val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMinY = range.minY + clampedPanY
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
        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        // Strict plotting area clipping starting at leftPadding to prevent mounting over the Y axis line
        canvas.withClip(
            leftPadding,
            topPadding,
            width - rightPadding + 4f * density,
            baselineY
        ) {
            for (dataset in visibleDatasets) {
                if (dataset.entries.isEmpty()) continue

                linePath.reset()
                fillPath.reset()

                val linePoints = FloatArray(dataset.entries.size * 2)
                var validCount = 0

                for (idx in dataset.entries.indices) {
                    val entry = dataset.entries[idx]
                    val pctX = if (entryCount > 1) {
                        if (range.rangeX > 0f) (entry.x - range.minX) / range.rangeX else idx.toFloat() / (entryCount - 1)
                    } else 0.5f
                    val screenX =
                        leftPadding + (pctX * drawWidth * effectiveZoomX) - clampedPanPixels
                    val pctY = (entry.y - effectiveMinY) / effectiveRangeY
                    val targetY = topPadding + topHeadroom + (1f - pctY) * drawHeight
                    val animatedY = baselineY - ((baselineY - targetY) * progress)

                    linePoints[idx * 2] = screenX
                    linePoints[idx * 2 + 1] = animatedY
                    validCount++

                    if (idx == 0) {
                        linePath.moveTo(screenX, animatedY)
                        fillPath.moveTo(screenX, baselineY)
                        fillPath.lineTo(screenX, animatedY)
                    } else {
                        if (dataset.isCurved) {
                            val prevX = linePoints[(idx - 1) * 2]
                            val prevY = linePoints[(idx - 1) * 2 + 1]
                            val controlX1 = prevX + (screenX - prevX) / 2f
                            val controlX2 = prevX + (screenX - prevX) / 2f
                            linePath.cubicTo(
                                controlX1,
                                prevY,
                                controlX2,
                                animatedY,
                                screenX,
                                animatedY
                            )
                            fillPath.cubicTo(
                                controlX1,
                                prevY,
                                controlX2,
                                animatedY,
                                screenX,
                                animatedY
                            )
                        } else {
                            linePath.lineTo(screenX, animatedY)
                            fillPath.lineTo(screenX, animatedY)
                        }
                    }
                }

                val lastX = linePoints[(validCount - 1) * 2]
                fillPath.lineTo(lastX, baselineY)
                fillPath.close()

                if (dataset.fillAlpha > 0f || dataset.gradientFill) {
                    val alpha =
                        if (dataset.fillAlpha > 0f) (dataset.fillAlpha * 255).toInt() else 60
                    val startColor = dataset.gradientStartColor
                        ?: ((alpha shl 24) or (dataset.color and 0x00FFFFFF))
                    val endColor = dataset.gradientEndColor ?: (0x00FFFFFF and dataset.color)
                    fillPaint.shader = LinearGradient(
                        0f, topPadding + topHeadroom, 0f, baselineY,
                        startColor, endColor, Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(fillPath, fillPaint)
                }

                linePaint.color = dataset.color
                linePaint.strokeWidth = dataset.lineWidth * density
                canvas.drawPath(linePath, linePaint)

                // Draw points
                for (idx in 0 until validCount) {
                    val px = linePoints[idx * 2]
                    val py = linePoints[idx * 2 + 1]
                    val entry = dataset.entries[idx]

                    if (dataset.showPoints) {
                        pointPaint.color = dataset.color
                        canvas.drawCircle(px, py, dataset.pointRadius * density, pointPaint)
                    }

                    val isSelected = (config.selectedEntry == entry)
                    if (isSelected) {
                        glassTooltipRenderer.drawHighlightPoint(
                            canvas,
                            px,
                            py,
                            dataset.color,
                            density
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
                    }

                    if ((config.showPointValues || isSelected) && progress >= 0.5f) {
                        val valueText = config.pointValueFormatter?.invoke(entry)
                            ?: entry.label
                            ?: formatValue(entry.y, config)

                        val cy = py - 22f * density
                        if (badgeCount < badgePool.size) {
                            val badge = badgePool[badgeCount]
                            badge.text = valueText
                            badge.x = px
                            badge.y = cy
                            badge.isSelected = isSelected
                        } else {
                            badgePool.add(PendingBadge(valueText, px, cy, isSelected))
                        }
                        badgeCount++
                    }
                }
            }
        }

        // PASS 2: Drawing X axis labels & vertical grid lines via AxisGridRenderer
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
