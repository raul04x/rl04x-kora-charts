package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Transforms data values into screen pixel coordinates supporting 2D Zoom & Pan.
 *
 * @param viewWidth Width of drawing area in px
 * @param viewHeight Height of drawing area in px
 * @param paddingPx Internal padding in px
 */
public class ChartEngine(
    private val viewWidth: Float,
    private val viewHeight: Float,
    private val paddingPx: Float = 48f,
) {
    private val drawWidth get() = (viewWidth - (paddingPx * 2))
    private val drawHeight get() = (viewHeight - (paddingPx * 2))

    /**
     * Computes the bounding data range from datasets.
     * Returns (minX, maxX, minY, maxY).
     */
    public fun computeRange(datasets: List<Dataset>): DataRange {
        val allEntries = datasets.flatMap { it.entries }
        if (allEntries.isEmpty()) return DataRange(0f, 1f, 0f, 1f)
        return DataRange(
            minX = allEntries.minOf { it.x },
            maxX = allEntries.maxOf { it.x },
            minY = allEntries.minOf { it.y },
            maxY = allEntries.maxOf { it.y },
        )
    }

    /**
     * Converts a data X value into a screen pixel X coordinate with 2D Zoom & Pan.
     */
    public fun toScreenX(
        x: Float,
        range: DataRange,
        zoomScaleX: Float = 1f,
        panOffsetX: Float = 0f,
    ): Float {
        val effectiveZoom = max(1f, zoomScaleX)
        if (range.rangeX == 0f) return paddingPx + (drawWidth / 2f)
        val visibleRangeX = if (effectiveZoom == 1f) range.rangeX else range.rangeX / effectiveZoom
        if (visibleRangeX == 0f) return paddingPx + (drawWidth / 2f)

        val maxPanOffset = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
        val clampedPanOffset = panOffsetX.coerceIn(0f, maxPanOffset)
        val visibleMinX = range.minX + clampedPanOffset

        return paddingPx + (x - visibleMinX) / visibleRangeX * drawWidth
    }

    /**
     * Converts a data Y value into a screen pixel Y coordinate with 2D Zoom & Pan.
     */
    public fun toScreenY(
        y: Float,
        range: DataRange,
        zoomScaleY: Float = 1f,
        panOffsetY: Float = 0f,
    ): Float {
        val effectiveZoom = max(1f, zoomScaleY)
        if (range.rangeY == 0f) return paddingPx + (drawHeight / 2f)
        val visibleRangeY = if (effectiveZoom == 1f) range.rangeY else range.rangeY / effectiveZoom
        if (visibleRangeY == 0f) return paddingPx + (drawHeight / 2f)

        val maxPanOffset = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanOffset = panOffsetY.coerceIn(0f, maxPanOffset)
        val visibleMinY = range.minY + clampedPanOffset

        // Inverted: Y=0 is at bottom in data space, top in Canvas
        return paddingPx + (1f - (y - visibleMinY) / visibleRangeY) * drawHeight
    }

    /** Converts screen pixel X coordinate back to data X value. */
    public fun toDataX(
        screenX: Float,
        range: DataRange,
        zoomScaleX: Float = 1f,
        panOffsetX: Float = 0f,
    ): Float {
        val effectiveZoom = max(1f, zoomScaleX)
        if (range.rangeX == 0f) return range.minX
        val visibleRangeX = if (effectiveZoom == 1f) range.rangeX else range.rangeX / effectiveZoom
        val maxPanOffset = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
        val clampedPanOffset = panOffsetX.coerceIn(0f, maxPanOffset)
        val visibleMinX = range.minX + clampedPanOffset

        return visibleMinX + (screenX - paddingPx) / drawWidth * visibleRangeX
    }

    /** Converts screen pixel Y coordinate back to data Y value. */
    public fun toDataY(
        screenY: Float,
        range: DataRange,
        zoomScaleY: Float = 1f,
        panOffsetY: Float = 0f,
    ): Float {
        val effectiveZoom = max(1f, zoomScaleY)
        if (range.rangeY == 0f) return range.minY
        val visibleRangeY = if (effectiveZoom == 1f) range.rangeY else range.rangeY / effectiveZoom
        val maxPanOffset = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanOffset = panOffsetY.coerceIn(0f, maxPanOffset)
        val visibleMinY = range.minY + clampedPanOffset

        return visibleMinY + (1f - (screenY - paddingPx) / drawHeight) * visibleRangeY
    }

    /** Finds the nearest [Entry] to a screen coordinate respecting 2D Zoom & Pan. */
    public fun nearestEntry(
        screenX: Float,
        screenY: Float,
        datasets: List<Dataset>,
        range: DataRange,
        zoomScaleX: Float = 1f,
        panOffsetX: Float = 0f,
        zoomScaleY: Float = 1f,
        panOffsetY: Float = 0f,
    ): Entry? {
        var nearest: Entry? = null
        var minDist = Float.MAX_VALUE
        for ((entries) in datasets) {
            for (entry in entries) {
                val ex = toScreenX(entry.x, range, zoomScaleX, panOffsetX)
                val ey = toScreenY(entry.y, range, zoomScaleY, panOffsetY)
                val dist = (screenX - ex) * (screenX - ex) + (screenY - ey) * (screenY - ey)
                if (dist < minDist) {
                    minDist = dist; nearest = entry
                }
            }
        }
        return nearest
    }

    /**
     * Finds the nearest [Entry] to a screen coordinate for Line Charts,
     * taking into account Cartesian plane (0,0) origin, exact line renderer padding, and 2D Zoom & Pan.
     */
    public fun nearestLineEntry(
        screenX: Float,
        screenY: Float,
        datasets: List<Dataset>,
        config: ChartConfig,
        density: Float,
    ): Entry? {
        val visibleDatasets = datasets.filter { it.visible && it.entries.isNotEmpty() }
        if (visibleDatasets.isEmpty()) return null

        val firstDataset = visibleDatasets.first()
        val entryCount = firstDataset.entries.size
        if (entryCount == 0) return null

        val range = computeRange(visibleDatasets)
        val maxX = maxOf(1f, range.maxX)

        val hasSecondaryY = config.showSecondaryYAxis || datasets.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && datasets.any { it.label.isNotEmpty() }

        val hasRotatedLabels = config.showAxisLabels && (
                config.xAxisLabelRotation != 0f || firstDataset.entries.any {
                    (it.label?.substringBefore('\n')?.length ?: 0) > 8
                }
                )

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density + 18f * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 16f * density)
        } else {
            config.paddingDp * density
        }

        val legendLabels = if (hasLegend) datasets.filter { it.visible && it.label.isNotEmpty() }
            .map { it.label } else emptyList()
        val legendRenderer = LegendRenderer()
        val legendHeight = if (hasLegend) legendRenderer.calculateLegendHeight(
            legendLabels,
            viewWidth - leftPadding - rightPadding,
            density
        ) else 0f
        val topPadding = config.paddingDp * density + legendHeight + 6f * density
        val topHeadroom = 10f * density

        val drawWidth = (viewWidth - leftPadding - rightPadding).coerceAtLeast(1f)
        val drawHeight = (viewHeight - topPadding - bottomPadding - topHeadroom).coerceAtLeast(1f)

        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY =
            if (visibleZoomY == 1f) maxOf(1f, range.maxY) else maxOf(1f, range.maxY) / visibleZoomY
        val maxPanY = (range.maxY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMaxY = clampedPanY + visibleRangeY

        val tickResult = if (config.useNiceTicks) {
            AxisTickCalculator.computeNiceTicks(
                min = clampedPanY,
                max = visibleMaxY,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMinY = 0f
        val effectiveMaxY = tickResult?.niceMax ?: visibleMaxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        var nearest: Entry? = null
        var minDist = Float.MAX_VALUE

        for ((entries) in visibleDatasets) {
            for (idx in entries.indices) {
                val entry = entries[idx]
                val pctX = entry.x / maxX

                val ex = leftPadding + (pctX * drawWidth * effectiveZoomX) - clampedPanPixels
                val pctY = entry.y / effectiveRangeY
                val ey = (topPadding + topHeadroom + drawHeight) - (pctY * drawHeight)

                val dist = (screenX - ex) * (screenX - ex) + (screenY - ey) * (screenY - ey)
                if (dist < minDist) {
                    minDist = dist
                    nearest = entry
                }
            }
        }

        return nearest
    }

    /**
     * Finds the pie or donut slice entry at screen coordinates (screenX, screenY).
     */
    public fun findPieEntryAt(
        screenX: Float,
        screenY: Float,
        datasets: List<Dataset>,
        density: Float = 1f,
        config: ChartConfig = ChartConfig(),
        holeRadiusRatio: Float = 0.55f,
    ): Entry? {
        val entries = datasets.firstOrNull()?.entries ?: return null
        val total = entries.sumOf { it.y.toDouble() }.toFloat()
        if (total <= 0f) return null

        val hasLegend = config.showLegend && entries.isNotEmpty()
        val legendHeight = if (hasLegend) {
            val labels = entries.mapIndexed { index, entry ->
                entry.label?.substringBefore('\n') ?: "Category ${index + 1}"
            }
            LegendRenderer().calculateLegendHeight(
                labels = labels,
                maxLegendWidth = viewWidth - paddingPx * 2f,
                density = density,
            )
        } else {
            0f
        }
        val layout = PieChartLayout.calculate(
            width = viewWidth,
            height = viewHeight,
            paddingPx = paddingPx,
            density = density,
            entryCount = entries.size,
            config = config,
            legendHeight = legendHeight,
        )
        val holeRadius = if (holeRadiusRatio > 0f) layout.radius * holeRadiusRatio else 0f

        var startAngle = -90f
        for (entry in entries) {
            val rawSweepAngle = (entry.y / total) * 360f
            val sweepAngle = if (rawSweepAngle in 0.001f..2.999f) 3f else rawSweepAngle
            if (sweepAngle <= 0f) {
                startAngle += rawSweepAngle
                continue
            }

            val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
            val explosionDist = if (entry == config.selectedEntry) 5f * density else 0f
            val sliceCenterX =
                layout.centerX + (explosionDist * cos(midAngleRad)).toFloat()
            val sliceCenterY =
                layout.centerY + (explosionDist * sin(midAngleRad)).toFloat()
            val dx = screenX - sliceCenterX
            val dy = screenY - sliceCenterY
            val distanceSquared = dx * dx + dy * dy
            val centerDx = screenX - layout.centerX
            val centerDy = screenY - layout.centerY
            val centerDistanceSquared = centerDx * centerDx + centerDy * centerDy

            if (
                distanceSquared <= layout.radius * layout.radius &&
                distanceSquared >= holeRadius * holeRadius &&
                centerDistanceSquared >= holeRadius * holeRadius
            ) {
                var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angleDeg < -90f) angleDeg += 360f
                if (angleDeg in startAngle..(startAngle + sweepAngle)) return entry
            }

            startAngle += rawSweepAngle
        }

        return null
    }
}

/** Data range bounding model calculated by [ChartEngine]. */
public data class DataRange(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
) {
    val rangeX: Float get() = maxX - minX
    val rangeY: Float get() = maxY - minY
}
