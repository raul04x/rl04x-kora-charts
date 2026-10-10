package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.renderer.helper.AxisGridRenderer

/**
 * Renderer for Bubble Scatter Plot charts.
 */
public class BubbleRenderer : BaseRenderer<BubbleEntry> {

    private val axisGridRenderer = AxisGridRenderer()

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
            if (config.showAxisLabels) config.paddingDp * density + 24f * density else config.paddingDp * density
        val bottomPadding =
            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
        val topPadding = config.paddingDp * density + 28f * density
        val rightPadding = config.paddingDp * density + 16f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight
        val minX = data.minOf { it.x }
        val maxX = data.maxOf { it.x }
        val rangeX = (maxX - minX).coerceAtLeast(1f)

        val minY = data.minOf { it.y }
        val maxY = data.maxOf { it.y }

        val tickResult = axisGridRenderer.drawYAxisGridAndLabels(
            canvas = canvas,
            config = config,
            minVal = minY,
            maxVal = maxY,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            density = density,
        )

        val effectiveMinY = tickResult?.niceMin ?: minY
        val effectiveMaxY = tickResult?.niceMax ?: maxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        // Draw translucent bubbles
        for ((x, y, radiusDp, baseColor, label) in data) {
            val cx = leftPadding + ((x - minX) / rangeX) * drawWidth
            val cy = baselineY - ((y - effectiveMinY) / effectiveRangeY) * drawHeight
            val radius = radiusDp * density * progress

            val alphaColor =
                Color.argb(140, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            bubbleFillPaint.color = alphaColor
            canvas.drawCircle(cx, cy, radius, bubbleFillPaint)

            bubbleStrokePaint.color = baseColor
            canvas.drawCircle(cx, cy, radius, bubbleStrokePaint)

            if (config.showAxisLabels && label != null) {
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
