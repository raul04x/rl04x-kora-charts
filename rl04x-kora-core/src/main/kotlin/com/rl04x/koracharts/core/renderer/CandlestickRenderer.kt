package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import java.util.Locale

/**
 * Renderer for financial Candlestick charts supporting 2D Zoom & Pan gestures, crosshairs, Glass tooltips, and zero allocations in draw().
 */
public class CandlestickRenderer : BaseRenderer<CandlestickEntry> {

    private val axisGridRenderer = AxisGridRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()

    private val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val candlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bullColor = "#10B981".toColorInt()
    private val bearColor = "#EF4444".toColorInt()

    private val cachedBodyRect = RectF()

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<CandlestickEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density

        val count = data.size

        val hasRotatedLabels = config.showAxisLabels && (
                config.xAxisLabelRotation != 0f || data.any { (it.label?.length ?: 0) > 8 }
                )
        val defaultRotation = if (hasRotatedLabels) {
            if (config.xAxisLabelRotation != 0f) config.xAxisLabelRotation else -90f
        } else 0f

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding = config.paddingDp * density + 10f * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }

        val topPadding = config.paddingDp * density + 10f * density
        val topHeadroom = 10f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding - topHeadroom
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + topHeadroom + drawHeight
        val minVal = data.minOf { it.low }
        val maxVal = data.maxOf { it.high }

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY =
            if (visibleZoomY == 1f) (maxVal - minVal) else (maxVal - minVal) / visibleZoomY
        val maxPanY = ((maxVal - minVal) - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMinY = minVal + clampedPanY
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

        val effectiveMinVal = tickResult?.niceMin ?: visibleMinY
        val effectiveMaxVal = tickResult?.niceMax ?: visibleMaxY
        val effectiveValRange = (effectiveMaxVal - effectiveMinVal).coerceAtLeast(1f)

        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val scaledSlotWidth = (drawWidth * effectiveZoomX) / count
        val candleWidth = (scaledSlotWidth * 0.55f).coerceAtLeast(4f * density)

        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        var selectedTooltipText: String? = null
        var selectedTooltipX = 0f
        var selectedTooltipY = 0f

        // PASS 1: Draw candles clipped to plotting area
        canvas.withClip(
            leftPadding,
            topPadding,
            width - rightPadding,
            baselineY
        ) {
            for ((idx, entry) in data.withIndex()) {
                val cx =
                    leftPadding + (idx * scaledSlotWidth) + (scaledSlotWidth / 2f) - clampedPanPixels
                val isBullish = entry.close >= entry.open
                val color = if (isBullish) bullColor else bearColor

                wickPaint.color = color
                candlePaint.color = color

                fun toY(v: Float): Float {
                    return topPadding + topHeadroom + (1f - (v - effectiveMinVal) / effectiveValRange) * drawHeight
                }

                val highY = toY(entry.high)
                val lowY = toY(entry.low)
                val openY = toY(entry.open)
                val closeY = toY(entry.close)

                canvas.drawLine(cx, highY, cx, lowY, wickPaint)

                val topBody = minOf(openY, closeY)
                val bottomBody = maxOf(openY, closeY)
                val bodyHeight = (bottomBody - topBody).coerceAtLeast(2f * density)

                val left = cx - (candleWidth / 2f)
                val right = cx + (candleWidth / 2f)
                cachedBodyRect.set(left, topBody, right, topBody + bodyHeight * progress)
                canvas.drawRoundRect(cachedBodyRect, 2f * density, 2f * density, candlePaint)

                val isSelected =
                    (config.selectedEntry?.x == entry.x || config.selectedEntry?.label == entry.label)
                if (isSelected) {
                    glassTooltipRenderer.drawCrosshairs(
                        canvas = canvas,
                        x = cx,
                        y = topBody,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        config = config,
                    )

                    val candleLabel =
                        entry.label ?: String.format(Locale.US, "Candle %.0f", entry.x)
                    selectedTooltipText =
                        "$candleLabel\nOpen: $${entry.open.toInt()} · High: $${entry.high.toInt()}\nLow: $${entry.low.toInt()} · Close: $${entry.close.toInt()}"
                    selectedTooltipX = cx
                    selectedTooltipY = highY - 12f * density
                }
            }
        }

        // PASS 2: Draw X axis labels aligned to candle centers
        if (config.showAxisLabels || config.showVerticalGrid) {
            val dummyEntries =
                data.map { com.rl04x.koracharts.core.model.Entry(it.x, it.close, it.label) }
            val minX = data.minOfOrNull { it.x } ?: 0f
            val maxX = data.maxOfOrNull { it.x } ?: (data.size - 1).toFloat()

            axisGridRenderer.drawXAxisGridAndLabels(
                canvas = canvas,
                config = config,
                entries = dummyEntries,
                minX = minX,
                maxX = maxX,
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
