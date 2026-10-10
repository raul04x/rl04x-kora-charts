package com.rl04x.koracharts.core.renderer.helper

import android.graphics.Canvas
import android.graphics.Paint
import com.rl04x.koracharts.core.model.ChartConfig

/**
 * Reusable helper renderer for single and multi-row word-wrapped legend bars.
 */
public class LegendRenderer {

    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8f
        textAlign = Paint.Align.LEFT
    }

    private val legendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /**
     * Calculates total height needed for multi-row word-wrapped legend bar.
     */
    public fun calculateLegendHeight(
        labels: List<String>,
        maxLegendWidth: Float,
        density: Float,
    ): Float {
        if (labels.isEmpty() || maxLegendWidth <= 0f) return 0f

        legendTextPaint.textSize = 8f * density
        var legendRows = 1
        val legendRowHeight = 15f * density
        var currLineWidth = 0f

        for (i in labels.indices) {
            val label = labels[i]
            val itemWidth = 14f * density + legendTextPaint.measureText(label) + 12f * density
            if (currLineWidth + itemWidth > maxLegendWidth && currLineWidth > 0f) {
                legendRows++
                currLineWidth = itemWidth
            } else {
                currLineWidth += itemWidth
            }
        }

        return (legendRows * legendRowHeight) + 4f * density
    }

    /**
     * Draws horizontal multi-row word-wrapped legend items.
     */
    public fun drawLegend(
        canvas: Canvas,
        labels: List<String>,
        colors: List<Int>,
        startX: Float,
        startY: Float,
        maxLegendWidth: Float,
        config: ChartConfig,
        density: Float,
    ) {
        if (labels.isEmpty() || maxLegendWidth <= 0f) return

        legendTextPaint.color = config.style.labelTextColor
        legendTextPaint.textSize = 8f * density

        var currX = startX
        var currY = startY
        val rowHeight = 15f * density

        for (i in labels.indices) {
            val label = labels[i]
            val color = colors.getOrElse(i) { 0xFF888888.toInt() }
            val itemWidth = 14f * density + legendTextPaint.measureText(label) + 12f * density

            if (currX + itemWidth > startX + maxLegendWidth && currX > startX) {
                currX = startX
                currY += rowHeight
            }

            legendDotPaint.color = color
            canvas.drawCircle(currX + 4f * density, currY, 3.5f * density, legendDotPaint)
            canvas.drawText(label, currX + 11f * density, currY + 3f * density, legendTextPaint)

            currX += itemWidth
        }
    }
}
