package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer

/**
 * Renderer for Bubble Scatter Plot charts on a Cartesian plane (0,0) origin with 2D touch inspection, axis headroom, and 2D zoom/pan.
 */
public class BubbleRenderer : BaseRenderer<BubbleEntry> {

    private val axisGridRenderer = AxisGridRenderer()
    private val glassTooltipRenderer = GlassTooltipRenderer()

    private val bubbleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<BubbleEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding = config.paddingDp * density + 18f * density
        val bottomPadding =
            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
        val topPadding = config.paddingDp * density + 10f * density
        val topHeadroom = 32f * density
        val marginHeadroom = 28f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding - topHeadroom
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val usableDrawWidth = (drawWidth - marginHeadroom).coerceAtLeast(1f)

        val maxX = data.maxOfOrNull { it.x }?.coerceAtLeast(1f) ?: 1f

        val maxY = data.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f

        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val maxPanPixels = (usableDrawWidth * effectiveZoomX - usableDrawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) maxY else maxY / visibleZoomY
        val maxPanY = (maxY - visibleRangeY).coerceAtLeast(0f)
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

        val effectiveMaxY = tickResult?.niceMax ?: visibleMaxY
        val effectiveRangeY = (effectiveMaxY - clampedPanY).coerceAtLeast(1f)
        val baselineY = topPadding + topHeadroom + drawHeight

        var selectedBadgeText: String? = null
        var selectedBadgeX = 0f
        var selectedBadgeY = 0f

        // PASS 1: Draw translucent bubbles clipped strictly to padded boundaries
        canvas.withClip(
            leftPadding,
            topPadding,
            width - rightPadding + 4f * density,
            baselineY + marginHeadroom
        ) {
            for ((x, y, radiusDp, baseColor, label) in data) {
                val pctX = x / maxX
                val cx =
                    leftPadding + marginHeadroom + (pctX * (usableDrawWidth - marginHeadroom) * effectiveZoomX) - clampedPanPixels
                val pctY = (y - clampedPanY) / effectiveRangeY
                val cy = baselineY - (pctY * drawHeight)
                val radius = radiusDp * density * progress

                val isSelected = (config.selectedEntry?.x == x && config.selectedEntry.y == y)

                val alphaColor = if (isSelected) {
                    Color.argb(
                        220,
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )
                } else {
                    Color.argb(
                        140,
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )
                }

                bubbleFillPaint.color = alphaColor
                canvas.drawCircle(cx, cy, radius, bubbleFillPaint)

                bubbleStrokePaint.color =
                    if (isSelected) config.style.highlightLineColor else baseColor
                bubbleStrokePaint.strokeWidth = if (isSelected) 3f * density else 1.5f * density
                canvas.drawCircle(cx, cy, radius, bubbleStrokePaint)

                if (isSelected) {
                    glassTooltipRenderer.drawCrosshairs(
                        canvas = canvas,
                        x = cx,
                        y = cy,
                        leftPadding = leftPadding,
                        topPadding = topPadding,
                        drawWidth = drawWidth,
                        drawHeight = drawHeight,
                        config = config,
                    )

                    selectedBadgeX = cx
                    selectedBadgeY = cy - radius - 12f * density
                    selectedBadgeText =
                        label?.let { "$it\nRisk: ${x.toInt()} · Return: ${y.toInt()}" }
                            ?: "Risk: ${x.toInt()} · Return: ${y.toInt()}"
                }
            }
        }

        // PASS 2: Draw X axis labels
        if (config.showAxisLabels) {
            for ((x, _, _, _, label) in data) {
                if (label != null) {
                    val pctX = x / maxX
                    val cx =
                        leftPadding + marginHeadroom + (pctX * (usableDrawWidth - marginHeadroom) * effectiveZoomX) - clampedPanPixels
                    if (cx in (leftPadding - 8f * density)..(width - rightPadding + 8f * density)) {
                        axisGridRenderer.drawXAxisLabel(
                            canvas = canvas,
                            label = label,
                            cx = cx,
                            baselineY = baselineY,
                            rotationDeg = config.xAxisLabelRotation,
                            config = config,
                            density = density,
                        )
                    }
                }
            }
        }

        // PASS 3: Draw selected badge on top of everything
        selectedBadgeText?.let { badgeText ->
            glassTooltipRenderer.drawGlassBadge(
                canvas = canvas,
                text = badgeText,
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
