package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.engine.PieChartLayout
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.renderer.helper.GlassTooltipRenderer
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import com.rl04x.koracharts.core.util.NumberFormatterUtils
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer for Pie and Donut charts with EVEN_ODD ring geometry, 360deg full circle safety,
 * system locale currency/number formatting, legend bar support, smart bounds callout text truncation,
 * glass tooltip badges, perfect inner hole circularity, and zero-allocation draw loops.
 */
public class PieRenderer(
    private val holeRadiusRatio: Float = 0.55f,
) : BaseRenderer<Dataset> {

    private val glassTooltipRenderer = GlassTooltipRenderer()
    private val legendRenderer = LegendRenderer()

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

    private val cachedOuterOval = RectF()
    private val cachedInnerOval = RectF()
    private val slicePath = Path()

    private class CalloutItem(
        var p1x: Float = 0f,
        var p1y: Float = 0f,
        var p2x: Float = 0f,
        var rawP2y: Float = 0f,
        var isRightSide: Boolean = true,
        var sliceColor: Int = 0,
        var textColor: Int = 0,
        var category: String = "",
        var percentage: String = "",
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

        val bgColor = if (Color.alpha(config.style.cardBackgroundColor) > 0) {
            config.style.cardBackgroundColor
        } else {
            "#0F172A".toColorInt()
        }
        holePaint.color = bgColor
        centerTitlePaint.color = config.style.titleTextColor
        centerSubtitlePaint.color = config.style.subtitleTextColor

        val density = Resources.getSystem().displayMetrics.density
        labelPaint.textSize = 12f * density
        centerTitlePaint.textSize = 20f * density
        centerSubtitlePaint.textSize = 13f * density

        rightCalloutCount = 0
        leftCalloutCount = 0

        val firstDataset = data.firstOrNull()
        val entries = firstDataset?.entries ?: return
        val total = entries.sumOf { it.y.toDouble() }.toFloat()
        if (total <= 0f) return

        val padding = config.paddingDp * density
        val hasLegend = config.showLegend && entries.isNotEmpty()

        val legendLabels = if (hasLegend) {
            entries.mapIndexed { i, e -> e.label?.substringBefore('\n') ?: "Category ${i + 1}" }
        } else emptyList()

        val legendColors = if (hasLegend) {
            entries.indices.map { i ->
                entries[i].color ?: firstDataset.colors?.getOrNull(i)
                ?: defaultColors[i % defaultColors.size]
            }
        } else emptyList()

        val legendHeight = if (hasLegend) {
            legendRenderer.calculateLegendHeight(legendLabels, width - (padding * 2f), density)
        } else 0f

        if (hasLegend) {
            legendRenderer.drawLegend(
                canvas = canvas,
                labels = legendLabels,
                colors = legendColors,
                startX = padding,
                startY = padding,
                maxLegendWidth = width - (padding * 2f),
                config = config,
                density = density,
            )
        }

        val layout = PieChartLayout.calculate(
            width = width,
            height = height,
            paddingPx = padding,
            density = density,
            entryCount = entries.size,
            config = config,
            legendHeight = legendHeight,
        )
        val radius = layout.radius
        val centerX = layout.centerX
        val centerY = layout.centerY

        val isDonut = holeRadiusRatio > 0f
        val holeRadius = if (isDonut) radius * holeRadiusRatio else 0f

        var startAngle = -90f
        var selectedBadgeText: String? = null
        var selectedBadgeX = 0f
        var selectedBadgeY = 0f

        for ((idx, entry) in entries.withIndex()) {
            val fullSweep = (entry.y / total) * 360f
            val rawSweepAngle = fullSweep * progress
            val sweepAngle = if (rawSweepAngle in 0.001f..2.999f) 3f else rawSweepAngle
            val isSelected = (config.selectedEntry == entry)
            val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())

            val sliceColor = entry.color
                ?: firstDataset.colors?.getOrNull(idx)
                ?: defaultColors[idx % defaultColors.size]

            val explosionDist = if (isSelected) 5f * density else 0f
            val shiftX = (explosionDist * cos(midAngleRad)).toFloat()
            val shiftY = (explosionDist * sin(midAngleRad)).toFloat()

            val sliceCenterX = centerX + shiftX
            val sliceCenterY = centerY + shiftY

            slicePath.reset()
            slicePath.fillType = Path.FillType.EVEN_ODD

            if (isDonut) {
                cachedOuterOval.set(
                    sliceCenterX - radius,
                    sliceCenterY - radius,
                    sliceCenterX + radius,
                    sliceCenterY + radius
                )
                cachedInnerOval.set(
                    sliceCenterX - holeRadius,
                    sliceCenterY - holeRadius,
                    sliceCenterX + holeRadius,
                    sliceCenterY + holeRadius
                )

                if (sweepAngle >= 359f) {
                    slicePath.addOval(cachedOuterOval, Path.Direction.CW)
                    slicePath.addOval(cachedInnerOval, Path.Direction.CW)
                } else {
                    slicePath.arcTo(cachedOuterOval, startAngle, sweepAngle)
                    slicePath.arcTo(cachedInnerOval, startAngle + sweepAngle, -sweepAngle)
                    slicePath.close()
                }
            } else {
                cachedOuterOval.set(
                    sliceCenterX - radius,
                    sliceCenterY - radius,
                    sliceCenterX + radius,
                    sliceCenterY + radius
                )
                if (sweepAngle >= 359f) {
                    slicePath.addOval(cachedOuterOval, Path.Direction.CW)
                } else {
                    slicePath.moveTo(sliceCenterX, sliceCenterY)
                    slicePath.arcTo(cachedOuterOval, startAngle, sweepAngle, false)
                    slicePath.close()
                }
            }

            slicePaint.style = Paint.Style.FILL
            slicePaint.color = sliceColor
            canvas.drawPath(slicePath, slicePaint)

            if (isSelected) {
                val badgeRadius = radius + 20f * density
                selectedBadgeX = (centerX + shiftX + badgeRadius * cos(midAngleRad)).toFloat()
                selectedBadgeY = (centerY + shiftY + badgeRadius * sin(midAngleRad)).toFloat()
                val pct = (entry.y / total) * 100f
                val categoryLabel = entry.label?.substringBefore('\n') ?: "Item"
                val formattedVal = formatValue(entry.y, config)
                selectedBadgeText = "$categoryLabel\n$formattedVal (${formatPercentage(pct)})"
            }

            if (config.showAxisLabels && progress >= 0.8f && !isSelected) {
                val pct = (entry.y / total) * 100f
                val pctText = formatPercentage(pct)

                if (sweepAngle > 18f) {
                    val midRadius =
                        if (isDonut) radius * (1f + holeRadiusRatio) / 2f else radius * 0.65f
                    val lx = (centerX + shiftX + midRadius * cos(midAngleRad)).toFloat()
                    val ly =
                        (centerY + shiftY + midRadius * sin(midAngleRad)).toFloat() + 4f * density

                    val calculatedContrastColor =
                        NumberFormatterUtils.calculateHarmoniousContrastColor(
                            sliceColor
                        )

                    labelPaint.color =
                        entry.textColor ?: firstDataset.labelTextColor ?: calculatedContrastColor
                    canvas.drawText(pctText, lx, ly, labelPaint)
                } else if (sweepAngle > 1f) {
                    val categoryLabel = entry.label?.substringBefore('\n') ?: "Item"
                    val p1x = (centerX + shiftX + radius * cos(midAngleRad)).toFloat()
                    val p1y = (centerY + shiftY + radius * sin(midAngleRad)).toFloat()
                    val lineDist = 12f * density
                    val p2x = (centerX + shiftX + (radius + lineDist) * cos(midAngleRad)).toFloat()
                    val p2y = (centerY + shiftY + (radius + lineDist) * sin(midAngleRad)).toFloat()

                    val isRightSide = cos(midAngleRad) >= 0
                    val textColor = entry.textColor ?: config.style.labelTextColor

                    if (isRightSide) {
                        if (rightCalloutCount < rightCallouts.size) {
                            val item = rightCallouts[rightCalloutCount]
                            item.p1x = p1x; item.p1y = p1y; item.p2x = p2x; item.rawP2y = p2y
                            item.isRightSide = true; item.sliceColor = sliceColor; item.textColor =
                                textColor; item.category = categoryLabel; item.percentage = pctText
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
                                    categoryLabel,
                                    pctText
                                )
                            )
                        }
                        rightCalloutCount++
                    } else {
                        if (leftCalloutCount < leftCallouts.size) {
                            val item = leftCallouts[leftCalloutCount]
                            item.p1x = p1x; item.p1y = p1y; item.p2x = p2x; item.rawP2y = p2y
                            item.isRightSide = false; item.sliceColor = sliceColor; item.textColor =
                                textColor; item.category = categoryLabel; item.percentage = pctText
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
                                    categoryLabel,
                                    pctText
                                )
                            )
                        }
                        leftCalloutCount++
                    }
                }
            }

            startAngle += rawSweepAngle
        }

        drawCallouts(canvas, rightCallouts, rightCalloutCount, height, width, density)
        drawCallouts(canvas, leftCallouts, leftCalloutCount, height, width, density)

        if (isDonut) {
            canvas.drawCircle(centerX, centerY, holeRadius, holePaint)

            val maxTextWidth = holeRadius * 1.6f
            val selected = config.selectedEntry
            val titleText = if (selected != null) {
                selected.label?.substringBefore('\n') ?: "Selected"
            } else {
                config.centerTitle ?: "Total"
            }

            val subtitleText = if (selected != null) {
                val pct = (selected.y / total) * 100f
                val formattedVal = formatValue(selected.y, config)
                "$formattedVal (${formatPercentage(pct)})"
            } else {
                config.centerSubtitle ?: formatValue(total, config)
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

    private fun formatValue(value: Float, config: ChartConfig): String {
        config.yAxisFormatter?.let { return it.invoke(value) }
        if (config.compactNumberFormatting) {
            return NumberFormatterUtils.formatCompact(
                value,
                config.valuePrefix,
                config.valueSuffix
            )
        }
        val numberFormat = NumberFormat.getInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (config.forceIntegerTicks) 0 else 1
            minimumFractionDigits = 0
        }
        val numStr = numberFormat.format(value.toDouble())
        return "${config.valuePrefix}$numStr${config.valueSuffix}"
    }

    private fun formatPercentage(pct: Float): String {
        val numberFormat = NumberFormat.getInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
        return "${numberFormat.format(pct.toDouble())}%"
    }

    private fun drawCallouts(
        canvas: Canvas,
        callouts: List<CalloutItem>,
        count: Int,
        height: Float,
        width: Float,
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

        calloutTextPaint.textSize = 10f * density

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

            val maxTextWidth = if (item.isRightSide) {
                (width - 6f * density - tx).coerceAtLeast(20f * density)
            } else {
                (tx - 6f * density).coerceAtLeast(20f * density)
            }

            val suffix = ": ${item.percentage}"
            var truncatedCategory = item.category
            if (calloutTextPaint.measureText("$truncatedCategory$suffix") > maxTextWidth) {
                while (
                    truncatedCategory.isNotEmpty() &&
                    calloutTextPaint.measureText("$truncatedCategory…$suffix") > maxTextWidth
                ) {
                    truncatedCategory = truncatedCategory.dropLast(1)
                }
            }
            val formattedText = when {
                truncatedCategory == item.category -> "$truncatedCategory$suffix"
                truncatedCategory.isNotEmpty() -> "$truncatedCategory…$suffix"
                else -> item.percentage
            }

            calloutTextPaint.color = item.textColor
            calloutTextPaint.textAlign =
                if (item.isRightSide) Paint.Align.LEFT else Paint.Align.RIGHT

            canvas.drawText(formattedText, tx, ty, calloutTextPaint)
        }
    }
}
