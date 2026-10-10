package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer for Pie and Donut charts with glassmorphic tooltip badges.
 */
public class PieRenderer(
    private val holeRadiusRatio: Float = 0.55f,
) : BaseRenderer<Dataset> {

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }

    private val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#222222".toColorInt()
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    private val centerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#666666".toColorInt()
        textSize = 13f
        textAlign = Paint.Align.CENTER
    }

    // Glassmorphism tooltip brushes
    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }

    private val calloutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private val calloutTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
    }

    private val textBoundsRect = Rect()

    private data class CalloutItem(
        val p1x: Float,
        val p1y: Float,
        val p2x: Float,
        val rawP2y: Float,
        val isRightSide: Boolean,
        val sliceColor: Int,
        val textColor: Int,
        val text: String,
    )

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<Dataset>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || (width <= 0f) || (height <= 0f)) return

        holePaint.color = config.style.cardBackgroundColor
        centerTitlePaint.color = config.style.titleTextColor
        centerSubtitlePaint.color = config.style.subtitleTextColor
        badgeTextPaint.color = config.style.tooltipTextColor

        val density = Resources.getSystem().displayMetrics.density
        labelPaint.textSize = 12f * density
        centerTitlePaint.textSize = 20f * density
        centerSubtitlePaint.textSize = 13f * density
        badgeTextPaint.textSize = 8f * density

        val padding = config.paddingDp * density
        val outerMargin = 12f * density

        val availableSize = minOf(width, height) - (padding * 2f) - (outerMargin * 2f)
        val size = maxOf(availableSize, 40f * density)

        val radius = size / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        val isDonut = holeRadiusRatio > 0f
        val ringWidth = if (isDonut) radius * (1f - holeRadiusRatio) else 0f
        val midRadius = if (isDonut) radius * (1f + holeRadiusRatio) / 2f else radius

        val oval = if (isDonut) {
            RectF(
                centerX - midRadius,
                centerY - midRadius,
                centerX + midRadius,
                centerY + midRadius
            )
        } else {
            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        }

        val firstDataset = data.firstOrNull()
        val entries = firstDataset?.entries ?: return
        val total = entries.sumOf { it.y.toDouble() }.toFloat()
        if (total <= 0f) return

        val defaultColors = listOf(
            "#14B8A6".toColorInt(), // Teal
            "#F87171".toColorInt(), // Coral
            "#F59E0B".toColorInt(), // Amber/Yellow
            "#8B5CF6".toColorInt(), // Purple
            "#2DD4BF".toColorInt(), // Cyan
            "#3B82F6".toColorInt(), // Blue
        )

        var startAngle = -90f
        var pendingSelectedBadge: Runnable? = null
        val rightCallouts = mutableListOf<CalloutItem>()
        val leftCallouts = mutableListOf<CalloutItem>()

        for ((idx, entry) in entries.withIndex()) {
            val sweepAngle = (entry.y / total) * 360f * progress
            val isSelected = (config.selectedEntry == entry)
            val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())

            val sliceColor = entry.color
                ?: firstDataset.colors?.getOrNull(idx)
                ?: defaultColors[idx % defaultColors.size]

            slicePaint.color = sliceColor

            if (isDonut) {
                slicePaint.style = Paint.Style.STROKE
                slicePaint.strokeWidth = ringWidth
            } else {
                slicePaint.style = Paint.Style.FILL
            }

            if (isSelected) {
                val explosionDist = 14f * density
                val shiftX = (explosionDist * cos(midAngleRad)).toFloat()
                val shiftY = (explosionDist * sin(midAngleRad)).toFloat()

                val shiftedOval = RectF(
                    oval.left + shiftX,
                    oval.top + shiftY,
                    oval.right + shiftX,
                    oval.bottom + shiftY,
                )

                canvas.drawArc(shiftedOval, startAngle, sweepAngle, !isDonut, slicePaint)

                // Prepare glassmorphic badge overlay
                val badgeRadius = radius + 20f * density
                val bx = (centerX + shiftX + badgeRadius * cos(midAngleRad)).toFloat()
                val by = (centerY + shiftY + badgeRadius * sin(midAngleRad)).toFloat()
                val pct = (entry.y / total) * 100f

                val baseLabel = entry.label ?: "Item"
                val badgeText = if (baseLabel.contains("\n")) {
                    baseLabel
                } else {
                    "$baseLabel: ${String.format(Locale.US, "%.0f%%", pct)}"
                }

                pendingSelectedBadge = Runnable {
                    val lines = badgeText.split("\n")
                    badgeTextPaint.color = config.style.tooltipTextColor
                    badgeTextPaint.textSize = 8f * density
                    badgeTextPaint.textAlign = Paint.Align.CENTER

                    var maxTextWidth = 0f
                    val lineSpacing = 16f * density

                    for (line in lines) {
                        badgeTextPaint.getTextBounds(line, 0, line.length, textBoundsRect)
                        val w = badgeTextPaint.measureText(line)
                        if (w > maxTextWidth) maxTextWidth = w
                    }
                    val totalTextHeight = lines.size * lineSpacing

                    val paddingX = config.tooltipPaddingXDp * density
                    val paddingY = config.tooltipPaddingYDp * density

                    val badgeWidth = maxTextWidth + (paddingX * 2f)
                    val badgeHeight = totalTextHeight + (paddingY * 2f)

                    val badgeRect = RectF(
                        bx - (badgeWidth / 2f),
                        by - (badgeHeight / 2f),
                        bx + (badgeWidth / 2f),
                        by + (badgeHeight / 2f),
                    )

                    // Screen border clamping
                    val screenMargin = 6f * density
                    if (badgeRect.left < screenMargin) badgeRect.offset(
                        screenMargin - badgeRect.left,
                        0f
                    )
                    if (badgeRect.right > width - screenMargin) badgeRect.offset(
                        (width - screenMargin) - badgeRect.right,
                        0f
                    )
                    if (badgeRect.top < screenMargin) badgeRect.offset(
                        0f,
                        screenMargin - badgeRect.top
                    )
                    if (badgeRect.bottom > height - screenMargin) badgeRect.offset(
                        0f,
                        (height - screenMargin) - badgeRect.bottom
                    )

                    val cornerPx = config.tooltipCornerRadiusDp * density
                    glassBgPaint.color = config.style.tooltipBackgroundColor
                    canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBgPaint)

                    if (config.showTooltipBorder) {
                        glassBorderPaint.strokeWidth = 1f * density
                        glassBorderPaint.color = config.style.tooltipBorderColor
                        canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBorderPaint)
                    }

                    val totalLines = lines.size
                    val fontCapHeight = 12f * density * 0.7f
                    var startY =
                        badgeRect.centerY() - ((totalLines - 1) * lineSpacing) / 2f + (fontCapHeight / 3.5f)
                    for (line in lines) {
                        canvas.drawText(line, badgeRect.centerX(), startY, badgeTextPaint)
                        startY += lineSpacing
                    }
                }
            } else {
                canvas.drawArc(oval, startAngle, sweepAngle, !isDonut, slicePaint)
            }

            if (config.showAxisLabels && progress >= 0.8f && !isSelected) {
                val pct = (entry.y / total) * 100f
                val pctText = String.format(Locale.US, "%.0f%%", pct)

                if (sweepAngle > 18f) {
                    val labelRadius = if (isDonut) midRadius else radius * 0.65f
                    val lx = (centerX + labelRadius * cos(midAngleRad)).toFloat()
                    val ly = (centerY + labelRadius * sin(midAngleRad)).toFloat() + 4f * density

                    val calculatedContrastColor =
                        com.rl04x.koracharts.core.util.NumberFormatterUtils.calculateHarmoniousContrastColor(
                            sliceColor
                        )

                    labelPaint.color =
                        entry.textColor ?: firstDataset.labelTextColor ?: calculatedContrastColor

                    canvas.drawText(pctText, lx, ly, labelPaint)
                } else if (sweepAngle > 1f) {
                    val p1x = (centerX + radius * cos(midAngleRad)).toFloat()
                    val p1y = (centerY + radius * sin(midAngleRad)).toFloat()

                    val lineDist = 12f * density
                    val p2x = (centerX + (radius + lineDist) * cos(midAngleRad)).toFloat()
                    val p2y = (centerY + (radius + lineDist) * sin(midAngleRad)).toFloat()

                    val isRightSide = cos(midAngleRad) >= 0
                    val textColor = entry.textColor ?: config.style.labelTextColor
                    val displayText =
                        entry.label?.substringBefore('\n')?.let { "$it: $pctText" } ?: pctText

                    val item = CalloutItem(
                        p1x = p1x,
                        p1y = p1y,
                        p2x = p2x,
                        rawP2y = p2y,
                        isRightSide = isRightSide,
                        sliceColor = sliceColor,
                        textColor = textColor,
                        text = displayText,
                    )

                    if (isRightSide) rightCallouts.add(item) else leftCallouts.add(item)
                }
            }

            startAngle += (entry.y / total) * 360f
        }

        // Resolve vertical Y collisions for external callout lines
        fun resolveAndDrawCallouts(items: List<CalloutItem>) {
            if (items.isEmpty()) return
            val sorted = items.sortedBy { it.rawP2y }
            val minGap = 13f * density
            val resultY = sorted.map { it.rawP2y }.toMutableList()

            // Forward pass: top to bottom push
            for (i in 1 until resultY.size) {
                if (resultY[i] < resultY[i - 1] + minGap) {
                    resultY[i] = resultY[i - 1] + minGap
                }
            }

            // Backward pass: bottom to top push if exceeding height
            val maxAllowedY = height - 10f * density
            val minAllowedY = 10f * density
            if (resultY.last() > maxAllowedY) {
                resultY[resultY.lastIndex] = maxAllowedY
                for (i in resultY.lastIndex - 1 downTo 0) {
                    if (resultY[i] > resultY[i + 1] - minGap) {
                        resultY[i] = resultY[i + 1] - minGap
                    }
                }
            }
            if (resultY.first() < minAllowedY) {
                resultY[0] = minAllowedY
                for (i in 1 until resultY.size) {
                    if (resultY[i] < resultY[i - 1] + minGap) {
                        resultY[i] = resultY[i - 1] + minGap
                    }
                }
            }

            for ((i, item) in sorted.withIndex()) {
                val adjustedY = resultY[i]
                val armLength = 10f * density
                val p3x = if (item.isRightSide) item.p2x + armLength else item.p2x - armLength

                calloutLinePaint.color = item.sliceColor
                calloutLinePaint.strokeWidth = 1.2f * density

                canvas.drawLine(item.p1x, item.p1y, item.p2x, adjustedY, calloutLinePaint)
                canvas.drawLine(item.p2x, adjustedY, p3x, adjustedY, calloutLinePaint)

                val tx = if (item.isRightSide) p3x + 4f * density else p3x - 4f * density
                val ty = adjustedY + 3.5f * density

                calloutTextPaint.color = item.textColor
                calloutTextPaint.textSize = 10f * density
                calloutTextPaint.textAlign =
                    if (item.isRightSide) Paint.Align.LEFT else Paint.Align.RIGHT

                canvas.drawText(item.text, tx, ty, calloutTextPaint)
            }
        }

        resolveAndDrawCallouts(rightCallouts)
        resolveAndDrawCallouts(leftCallouts)

        if (isDonut) {
            val holeRadius = radius * holeRadiusRatio
            if (Color.alpha(config.style.cardBackgroundColor) > 0) {
                canvas.drawCircle(centerX, centerY, holeRadius, holePaint)
            }

            // Safe text bounds strictly inside donut hole (80% of inner diameter)
            val maxTextWidth = holeRadius * 1.6f

            val selected = config.selectedEntry
            val titleText = if (selected != null) {
                selected.label?.substringBefore('\n') ?: "Selected"
            } else {
                config.centerTitle ?: "Total"
            }

            val subtitleText = if (selected != null) {
                val pct = (selected.y / total) * 100f
                String.format(Locale.US, "%.1f (%.0f%%)", selected.y, pct)
            } else {
                config.centerSubtitle ?: String.format(Locale.US, "%.0f", total)
            }

            // Auto-scale title font size to stay inside donut hole
            var titleSize = 16f * density
            centerTitlePaint.textSize = titleSize
            while (centerTitlePaint.measureText(titleText) > maxTextWidth && titleSize > 9f * density) {
                titleSize -= 1f * density
                centerTitlePaint.textSize = titleSize
            }

            var formattedTitle = titleText
            if (centerTitlePaint.measureText(formattedTitle) > maxTextWidth) {
                while (formattedTitle.length > 3 && centerTitlePaint.measureText("$formattedTitle...") > maxTextWidth) {
                    formattedTitle = formattedTitle.dropLast(1)
                }
                formattedTitle = "$formattedTitle..."
            }

            // Auto-scale subtitle font size
            var subtitleSize = 12f * density
            centerSubtitlePaint.textSize = subtitleSize
            while (centerSubtitlePaint.measureText(subtitleText) > maxTextWidth && subtitleSize > 8f * density) {
                subtitleSize -= 1f * density
                centerSubtitlePaint.textSize = subtitleSize
            }

            val titleY = centerY - 2f * density
            val subtitleY = centerY + subtitleSize + 3f * density

            canvas.drawText(formattedTitle, centerX, titleY, centerTitlePaint)
            canvas.drawText(subtitleText, centerX, subtitleY, centerSubtitlePaint)
        }

        // Draw overlay badge on top of everything
        pendingSelectedBadge?.run()
    }
}
