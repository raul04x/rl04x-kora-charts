package com.rl04x.koracharts.core.renderer.helper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.rl04x.koracharts.core.model.ChartConfig

/**
 * Reusable helper renderer for glass tooltip badges, crosshair highlight lines,
 * and point selection indicators with full multi-line (`\n`) left-aligned support without
 * allocating objects during canvas draw operations.
 */
public class GlassTooltipRenderer {

    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f
        textAlign = Paint.Align.LEFT
    }

    private val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val highlightPointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val highlightPointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeRect = RectF()
    private val textBoundsRect = Rect()

    /**
     * Draws crosshair highlight lines (dashed horizontal / vertical) at (x, y).
     */
    public fun drawCrosshairs(
        canvas: Canvas,
        x: Float,
        y: Float,
        leftPadding: Float,
        topPadding: Float,
        drawWidth: Float,
        drawHeight: Float,
        config: ChartConfig,
    ) {
        if (!config.showHighlightLine) return

        highlightLinePaint.color = config.style.highlightLineColor

        val baselineY = topPadding + drawHeight
        val rightX = leftPadding + drawWidth

        if (config.showHighlightLineX && x in leftPadding..rightX) {
            canvas.drawLine(x, topPadding, x, baselineY, highlightLinePaint)
        }
        if (config.showHighlightLineY && y in topPadding..baselineY) {
            canvas.drawLine(leftPadding, y, rightX, y, highlightLinePaint)
        }
    }

    /**
     * Draws selection point indicator at (x, y).
     */
    public fun drawHighlightPoint(
        canvas: Canvas,
        x: Float,
        y: Float,
        pointColor: Int,
        density: Float,
    ) {
        highlightPointOuterPaint.color = Color.WHITE
        highlightPointInnerPaint.color = pointColor

        canvas.drawCircle(x, y, 7f * density, highlightPointOuterPaint)
        canvas.drawCircle(x, y, 5f * density, highlightPointInnerPaint)
    }

    /**
     * Draws a glass tooltip badge with translucent background and left-aligned multi-line (`\n`) text.
     */
    public fun drawGlassBadge(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        canvasWidth: Float,
        config: ChartConfig,
        density: Float,
        isSelected: Boolean = false,
    ) {
        if (text.isEmpty() || !config.showTooltip) return

        val rawBgColor = config.style.tooltipBackgroundColor
        // Enforce 85% glass translucency (0xD9) if an opaque 24-bit color was supplied
        val glassBgColor = if (Color.alpha(rawBgColor) == 255) {
            (rawBgColor and 0x00FFFFFF) or 0xD9000000.toInt()
        } else {
            rawBgColor
        }

        glassBgPaint.color = glassBgColor
        glassBorderPaint.color =
            if (isSelected) config.style.highlightLineColor else config.style.tooltipBorderColor
        glassBorderPaint.strokeWidth = if (isSelected) 1.5f * density else 1f * density

        badgeTextPaint.color = config.style.tooltipTextColor
        badgeTextPaint.textSize = 8f * density
        badgeTextPaint.textAlign = Paint.Align.LEFT

        val lines = text.split("\n")
        var maxTextWidth = 0f
        val lineSpacing = 12f * density

        for (line in lines) {
            val w = badgeTextPaint.measureText(line)
            if (w > maxTextWidth) maxTextWidth = w
        }

        val totalTextHeight = if (lines.size == 1) {
            badgeTextPaint.getTextBounds(lines[0], 0, lines[0].length, textBoundsRect)
            textBoundsRect.height().toFloat()
        } else {
            lines.size * lineSpacing
        }

        val paddingX = config.tooltipPaddingXDp * density
        val paddingY = config.tooltipPaddingYDp * density
        val cornerRadius = config.tooltipCornerRadiusDp * density

        val badgeWidth = maxTextWidth + (paddingX * 2f)
        val badgeHeight = totalTextHeight + (paddingY * 2f)

        var badgeLeft = cx - (badgeWidth / 2f)
        var badgeTop = cy - badgeHeight - (6f * density)

        // Clamping within screen bounds
        if (badgeLeft < 8f * density) badgeLeft = 8f * density
        if (badgeLeft + badgeWidth > canvasWidth - 8f * density) {
            badgeLeft = canvasWidth - 8f * density - badgeWidth
        }
        if (badgeTop < 8f * density) {
            badgeTop = cy + (10f * density)
        }

        badgeRect.set(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

        // Render translucent background card
        canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, glassBgPaint)
        if (config.showTooltipBorder) {
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, glassBorderPaint)
        }

        val textX = badgeLeft + paddingX
        if (lines.size == 1) {
            val textY = badgeTop + paddingY + totalTextHeight - (1f * density)
            canvas.drawText(lines[0], textX, textY, badgeTextPaint)
        } else {
            val startY = badgeTop + paddingY + (lineSpacing * 0.7f)
            for (i in lines.indices) {
                canvas.drawText(lines[i], textX, startY + (i * lineSpacing), badgeTextPaint)
            }
        }
    }
}
