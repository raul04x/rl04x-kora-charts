package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer for Pie and Donut charts with glassmorphic tooltip badges and zero-allocation draw loops.
 */
public class PieRenderer(
    private val holeRadiusRatio: Float = 0.55f,
) : BaseRenderer<Dataset> {

    private val glassTooltipRenderer = GlassTooltipRenderer()

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val holePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 12f; textAlign = Paint.Align.CENTER
    }
    private val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#222222".toColorInt(); textSize = 20f; textAlign = Paint.Align.CENTER
    }
    private val centerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#666666".toColorInt(); textSize = 13f; textAlign = Paint.Align.CENTER
    }
    private val calloutLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f }
    private val calloutTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }

    private val cachedOval = RectF()
    private val cachedShiftedOval = RectF()

    private class CalloutItem(
        var p1x: Float = 0f,
        var p1y: Float = 0f,
        var p2x: Float = 0f,
        var rawP2y: Float = 0f,
        var isRightSide: Boolean = true,
        var sliceColor: Int = 0,
        var textColor: Int = 0,
        var text: String = "",
    )

    private val rightCallouts =
        ArrayList<CalloutItem>(16).apply { repeat(16) { add(CalloutItem()) } }
    private val leftCallouts =
        ArrayList<CalloutItem>(16).apply { repeat(16) { add(CalloutItem()) } }
    private var rightCalloutCount = 0
    private var leftCalloutCount = 0

    private val defaultColors = listOf(
        "#14B8A6".toColorInt(),
        "#F87171".toColorInt(),
        "#F59E0B".toColorInt(),
        "#8B5CF6".toColorInt(),
        "#2DD4BF".toColorInt(),
        "#3B82F6".toColorInt(),
    )

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<Dataset>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        holePaint.color = config.style.cardBackgroundColor
        centerTitlePaint.color = config.style.titleTextColor
        centerSubtitlePaint.color = config.style.subtitleTextColor

        val density = Resources.getSystem().displayMetrics.density
        labelPaint.textSize = 12f * density
        centerTitlePaint.textSize = 20f * density
        centerSubtitlePaint.textSize = 13f * density

        rightCalloutCount = 0
        leftCalloutCount = 0

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

        if (isDonut) {
            cachedOval.set(
                centerX - midRadius,
                centerY - midRadius,
                centerX + midRadius,
                centerY + midRadius
            )
        } else {
            cachedOval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        }

        val firstDataset = data.firstOrNull()
        val entries = firstDataset?.entries ?: return
        val total = entries.sumOf { it.y.toDouble() }.toFloat()
        if (total <= 0f) return

        var startAngle = -90f
        var selectedBadgeText: String? = null
        var selectedBadgeX = 0f
        var selectedBadgeY = 0f

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

                cachedShiftedOval.set(
                    cachedOval.left + shiftX,
                    cachedOval.top + shiftY,
                    cachedOval.right + shiftX,
                    cachedOval.bottom + shiftY,
                )

                canvas.drawArc(cachedShiftedOval, startAngle, sweepAngle, !isDonut, slicePaint)

                val badgeRadius = radius + 20f * density
                selectedBadgeX = (centerX + shiftX + badgeRadius * cos(midAngleRad)).toFloat()
                selectedBadgeY = (centerY + shiftY + badgeRadius * sin(midAngleRad)).toFloat()
                val pct = (entry.y / total) * 100f
                val baseLabel = entry.label ?: "Item"
                selectedBadgeText = if (baseLabel.contains("\n")) {
                    baseLabel
                } else {
                    "$baseLabel: ${String.format(Locale.US, "%.0f%%", pct)}"
                }
            } else {
                canvas.drawArc(cachedOval, startAngle, sweepAngle, !isDonut, slicePaint)
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

                    if (isRightSide) {
                        if (rightCalloutCount < rightCallouts.size) {
                            val item = rightCallouts[rightCalloutCount]
                            item.p1x = p1x; item.p1y = p1y; item.p2x = p2x; item.rawP2y = p2y
                            item.isRightSide = true; item.sliceColor = sliceColor; item.textColor =
                                textColor; item.text = displayText
                        } else {
                            rightCallouts.add(
                                CalloutItem(
                                    p1x,
                                    p1y,
                                    p2x,
                                    p2y,
                                    true,
                                    sliceColor,
                                    textColor,
                                    displayText
                                )
                            )
                        }
                        rightCalloutCount++
                    } else {
                        if (leftCalloutCount < leftCallouts.size) {
                            val item = leftCallouts[leftCalloutCount]
                            item.p1x = p1x; item.p1y = p1y; item.p2x = p2x; item.rawP2y = p2y
                            item.isRightSide = false; item.sliceColor = sliceColor; item.textColor =
                                textColor; item.text = displayText
                        } else {
                            leftCallouts.add(
                                CalloutItem(
                                    p1x,
                                    p1y,
                                    p2x,
                                    p2y,
                                    false,
                                    sliceColor,
                                    textColor,
                                    displayText
                                )
                            )
                        }
                        leftCalloutCount++
                    }
                }
            }

            startAngle += (entry.y / total) * 360f
        }

        drawCallouts(canvas, rightCallouts, rightCalloutCount, height, density)
        drawCallouts(canvas, leftCallouts, leftCalloutCount, height, density)

        if (isDonut) {
            val holeRadius = radius * holeRadiusRatio
            if (Color.alpha(config.style.cardBackgroundColor) > 0) {
                canvas.drawCircle(centerX, centerY, holeRadius, holePaint)
            }

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

    private fun drawCallouts(
        canvas: Canvas,
        callouts: List<CalloutItem>,
        count: Int,
        height: Float,
        density: Float,
    ) {
        if (count == 0) return
        val items = callouts.subList(0, count).sortedBy { it.rawP2y }
        val minGap = 13f * density
        val resultY = FloatArray(count) { items[it].rawP2y }

        for (i in 1 until count) {
            if (resultY[i] < resultY[i - 1] + minGap) {
                resultY[i] = resultY[i - 1] + minGap
            }
        }

        val maxAllowedY = height - 10f * density
        val minAllowedY = 10f * density
        if (resultY[count - 1] > maxAllowedY) {
            resultY[count - 1] = maxAllowedY
            for (i in count - 2 downTo 0) {
                if (resultY[i] > resultY[i + 1] - minGap) {
                    resultY[i] = resultY[i + 1] - minGap
                }
            }
        }
        if (resultY[0] < minAllowedY) {
            resultY[0] = minAllowedY
            for (i in 1 until count) {
                if (resultY[i] < resultY[i - 1] + minGap) {
                    resultY[i] = resultY[i - 1] + minGap
                }
            }
        }

        for (i in 0 until count) {
            val item = items[i]
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
}
