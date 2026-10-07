package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

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
    private val drawWidth  get() = (viewWidth  - (paddingPx * 2))
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
        for (ds in datasets) {
            for (entry in ds.entries) {
                val ex = toScreenX(entry.x, range, zoomScaleX, panOffsetX)
                val ey = toScreenY(entry.y, range, zoomScaleY, panOffsetY)
                val dist = (screenX - ex) * (screenX - ex) + (screenY - ey) * (screenY - ey)
                if (dist < minDist) { minDist = dist; nearest = entry }
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
        holeRadiusRatio: Float = 0.55f,
        density: Float = 1f,
    ): Entry? {
        val entries = datasets.firstOrNull()?.entries ?: return null
        val total = entries.sumOf { it.y.toDouble() }.toFloat()
        if (total <= 0f) return null

        val padding = paddingPx
        val outerMargin = 12f * density
        val availableSize = minOf(viewWidth, viewHeight) - (padding * 2f) - (outerMargin * 2f)
        val size = maxOf(availableSize, 40f * density)

        val radius = size / 2f
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val dx = screenX - centerX
        val dy = screenY - centerY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist > radius + 40f * density) return null

        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angleDeg < -90f) {
            angleDeg += 360f
        }

        var startAngle = -90f
        for (entry in entries) {
            val sweepAngle = (entry.y / total) * 360f
            val endAngle = startAngle + sweepAngle
            if (angleDeg in startAngle..endAngle) {
                return entry
            }
            startAngle = endAngle
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
