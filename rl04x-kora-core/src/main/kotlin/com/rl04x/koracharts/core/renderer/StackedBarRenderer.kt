package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.StackedBarEntry
import java.util.Locale

/**
 * Renderer for Stacked Bar Charts supporting top-only rounded corners, segment value labels,
 * multi-row word-wrap legend bar, 2D Zoom & Pan gestures, crosshairs, and Glassmorphic badge tooltips.
 */
public class StackedBarRenderer : BaseRenderer<StackedBarEntry> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    private val segmentValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val highlightBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    // Legend brushes
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8f
        textAlign = Paint.Align.LEFT
    }

    private val legendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
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
        textSize = 9f
        textAlign = Paint.Align.LEFT
    }

    private val textBoundsRect = Rect()

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<StackedBarEntry>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        gridPaint.color = config.style.gridColor
        axisPaint.color = config.style.axisColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 9f * density
        segmentValuePaint.textSize = 9f * density
        totalValuePaint.color = config.style.titleTextColor
        totalValuePaint.textSize = 10f * density
        legendTextPaint.color = config.style.labelTextColor
        legendTextPaint.textSize = 8f * density
        highlightLinePaint.color = config.style.highlightLineColor
        highlightBorderPaint.color = config.style.highlightLineColor

        val firstEntry = data.first()
        val hasLegend = config.showLegend && firstEntry.colors.isNotEmpty()

        val hasRotatedLabels =
            config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.any {
                (it.label?.length ?: 0) > 8
            })

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding = config.paddingDp * density + 10f * density
        val maxLegendLineWidth = (width - leftPadding - rightPadding).coerceAtLeast(1f)

        // Calculate multi-row word-wrap legend height with 8dp font
        var legendRows = 1
        val legendRowHeight = 15f * density
        if (hasLegend) {
            var currLineWidth = 0f
            for (cIdx in firstEntry.colors.indices) {
                val legendLabel =
                    firstEntry.segmentLabels?.getOrNull(cIdx) ?: "Series ${cIdx + 1}"
                val itemWidth =
                    14f * density + legendTextPaint.measureText(legendLabel) + 12f * density
                if (currLineWidth + itemWidth > maxLegendLineWidth && currLineWidth > 0f) {
                    legendRows++
                    currLineWidth = itemWidth
                } else {
                    currLineWidth += itemWidth
                }
            }
        }

        val totalLegendHeight = if (hasLegend) (legendRows * legendRowHeight) + 4f * density else 0f

        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding =
            config.paddingDp * density + (if (hasLegend) totalLegendHeight + 4f * density else 12f * density) + 6f * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val baselineY = topPadding + drawHeight
        val totals = data.map { it.values.sum() }
        val maxTotal = totals.maxOrNull()?.coerceAtLeast(1f) ?: 100f

        // 2D Zoom & Pan calculation for Y range
        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) maxTotal else maxTotal / visibleZoomY
        val maxPanY = (maxTotal - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)

        val visibleMaxY = clampedPanY + visibleRangeY

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = clampedPanY,
                max = visibleMaxY,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMinY = yTickResult?.niceMin ?: clampedPanY
        val effectiveMaxY = yTickResult?.niceMax ?: visibleMaxY
        val effectiveValRange = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        fun toScreenY(yVal: Float): Float {
            return topPadding + (1f - (yVal - effectiveMinY) / effectiveValRange) * drawHeight
        }

        fun formatYValue(value: Float): String {
            if (config.yAxisFormatter != null) return config.yAxisFormatter.invoke(value)
            if (config.compactNumberFormatting) {
                return com.rl04x.koracharts.core.util.NumberFormatterUtils.formatCompact(
                    value,
                    config.valuePrefix,
                    config.valueSuffix
                )
            }
            val numStr = if (config.forceIntegerTicks) String.format(
                Locale.US,
                "%.0f",
                value
            ) else String.format(Locale.US, "%.1f", value)
            return "${config.valuePrefix}$numStr${config.valueSuffix}"
        }

        // Draw Multi-row Word-wrap Legend at top if enabled
        if (hasLegend) {
            var currentX = leftPadding
            var currentLegendY = config.paddingDp * density + 12f * density

            for (cIdx in firstEntry.colors.indices) {
                val color = firstEntry.colors[cIdx]
                val legendLabel =
                    firstEntry.segmentLabels?.getOrNull(cIdx) ?: "Series ${cIdx + 1}"
                val labelWidth = legendTextPaint.measureText(legendLabel)
                val itemWidth = 14f * density + labelWidth + 12f * density

                if (currentX + itemWidth > width - rightPadding && currentX > leftPadding) {
                    currentX = leftPadding
                    currentLegendY += legendRowHeight
                }

                legendDotPaint.color = color
                canvas.drawCircle(
                    currentX + 4f * density,
                    currentLegendY - 3.5f * density,
                    4f * density,
                    legendDotPaint
                )

                canvas.drawText(
                    legendLabel,
                    currentX + 11f * density,
                    currentLegendY,
                    legendTextPaint
                )

                currentX += itemWidth
            }
        }

        // Draw Target Zones
        for (zone in config.targetZones) {
            val topPct = ((zone.maxY - effectiveMinY) / effectiveValRange).coerceIn(0f, 1f)
            val bottomPct = ((zone.minY - effectiveMinY) / effectiveValRange).coerceIn(0f, 1f)
            val zTop = topPadding + (1f - topPct) * drawHeight
            val zBottom = topPadding + (1f - bottomPct) * drawHeight

            val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = zone.color
                alpha = (zone.fillAlpha * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftPadding, zTop, width - rightPadding, zBottom, zonePaint)

            if (zone.label != null) {
                val zoneTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = zone.color
                    textSize = 9f * density
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(
                    zone.label,
                    leftPadding + 6f * density,
                    zTop + 12f * density,
                    zoneTextPaint
                )
            }
        }

        // Draw Reference Lines
        for (refLine in config.referenceLines) {
            val refY = toScreenY(refLine.value)
            if (refY in topPadding..baselineY) {
                val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = refLine.color
                    strokeWidth = refLine.strokeWidthDp * density
                    style = Paint.Style.STROKE
                    if (refLine.isDashed) {
                        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                    }
                }
                canvas.drawLine(leftPadding, refY, width - rightPadding, refY, refPaint)

                if (refLine.label != null) {
                    val refTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = refLine.color
                        textSize = 9f * density
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(
                        refLine.label,
                        width - rightPadding - 6f * density,
                        refY - 4f * density,
                        refTextPaint
                    )
                }
            }
        }

        // Draw Average Line
        if (config.showAverageLine && totals.isNotEmpty()) {
            val avgY = totals.average().toFloat()
            val avgScreenY = toScreenY(avgY)
            if (avgScreenY in topPadding..baselineY) {
                val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.averageLineColor ?: config.style.highlightLineColor
                    strokeWidth = 1.5f * density
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
                }
                canvas.drawLine(leftPadding, avgScreenY, width - rightPadding, avgScreenY, avgPaint)

                val avgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.averageLineColor ?: config.style.highlightLineColor
                    textSize = 9f * density
                    textAlign = Paint.Align.LEFT
                }
                val avgText = "Avg: ${formatYValue(avgY)}"
                canvas.drawText(
                    avgText,
                    leftPadding + 6f * density,
                    avgScreenY - 4f * density,
                    avgTextPaint
                )
            }
        }

        // Draw horizontal grid lines
        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val y = toScreenY(yVal)
                    if (y in (topPadding - 1f)..(baselineY + 1f)) {
                        canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                        if (config.showAxisLabels) {
                            val labelText = formatYValue(yVal)
                            canvas.drawText(
                                labelText,
                                leftPadding - 6f * density,
                                y + 4f * density,
                                labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        }
                    }
                }
            } else {
                val steps = 4
                for (i in 0..steps) {
                    val y = topPadding + (drawHeight * (i.toFloat() / steps))
                    canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                    if (config.showAxisLabels) {
                        val valY = visibleMaxY - (visibleRangeY * (i.toFloat() / steps))
                        canvas.drawText(
                            formatYValue(valY),
                            leftPadding - 6f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }
        }

        if (config.showAxes) {
            canvas.drawLine(leftPadding, baselineY, width - rightPadding, baselineY, axisPaint)
            canvas.drawLine(leftPadding, topPadding, leftPadding, baselineY, axisPaint)
        }

        val entryCount = data.size
        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val slotWidth = (drawWidth * effectiveZoomX) / entryCount
        val barWidth = slotWidth * 0.52f

        var selectedStackedEntry: StackedBarEntry? = null
        var selectedBarCenterX = 0f
        var selectedBarTopY = 0f

        canvas.withClip(
            leftPadding - 12f * density,
            topPadding - 16f * density,
            width - rightPadding + 16f * density,
            baselineY + 8f * density
        ) {
            for ((eIdx, entry) in data.withIndex()) {
                val rawCenterX = leftPadding + (eIdx * slotWidth) + (slotWidth / 2f)
                val slotCenterX = rawCenterX - config.panOffsetX
                val barLeft = slotCenterX - (barWidth / 2f)
                val barRight = slotCenterX + (barWidth / 2f)

                if (barRight < leftPadding || barLeft > width - rightPadding) continue

                val isSelected = config.selectedEntry != null &&
                        (config.selectedEntry.x == entry.x || config.selectedEntry.label == entry.label)

                if (isSelected) {
                    selectedStackedEntry = entry
                    selectedBarCenterX = slotCenterX
                }

                var runningVal = 0f
                var currentTopY = baselineY
                val overallTotal = entry.values.sum()

                for ((vIdx, valItem) in entry.values.withIndex()) {
                    val segBottomVal = runningVal
                    runningVal += valItem
                    val segTopVal = runningVal

                    val segBottomY = toScreenY(segBottomVal)
                    val segTopY = toScreenY(segTopVal)
                    val segmentHeight = (segBottomY - segTopY) * progress
                    val segmentTop = segBottomY - segmentHeight

                    currentTopY = segmentTop

                    segmentPaint.color =
                        entry.colors.getOrElse(vIdx) { config.style.highlightLineColor }
                    val rect = RectF(barLeft, segmentTop, barRight, segBottomY)

                    val isTopSegment = (vIdx == entry.values.lastIndex)
                    if (isTopSegment) {
                        val r = 6f * density
                        val path = Path().apply {
                            addRoundRect(
                                rect,
                                floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f),
                                Path.Direction.CW,
                            )
                        }
                        canvas.drawPath(path, segmentPaint)
                    } else {
                        canvas.drawRect(rect, segmentPaint)
                    }

                    // Segment value label
                    if (segmentHeight >= 10f * density) {
                        val segmentColor =
                            entry.colors.getOrElse(vIdx) { config.style.highlightLineColor }
                        val customTextColor = entry.textColors?.getOrNull(vIdx)

                        val contrastTextColor =
                            customTextColor
                                ?: com.rl04x.koracharts.core.util.NumberFormatterUtils.calculateHarmoniousContrastColor(
                                    segmentColor
                                )

                        val dynamicFontSize =
                            minOf(12f * density, maxOf(9f * density, segmentHeight * 0.75f))
                        segmentValuePaint.textSize = dynamicFontSize
                        segmentValuePaint.color = contrastTextColor

                        val valText = formatYValue(valItem)
                        canvas.drawText(
                            valText,
                            slotCenterX,
                            segmentTop + (segmentHeight / 2f) + (dynamicFontSize / 3f),
                            segmentValuePaint
                        )
                    }
                }

                if (isSelected) {
                    selectedBarTopY = currentTopY
                    val r = 6f * density
                    val barRect = RectF(barLeft, currentTopY, barRight, baselineY)
                    canvas.drawRoundRect(barRect, r, r, highlightBorderPaint)
                }

                // Draw total value label on top of the bar
                canvas.drawText(
                    formatYValue(overallTotal),
                    slotCenterX,
                    currentTopY - 6f * density,
                    totalValuePaint,
                )
            }

            // Crosshair highlight line
            if (selectedStackedEntry != null && config.showHighlightLineX) {
                canvas.drawLine(
                    selectedBarCenterX,
                    topPadding,
                    selectedBarCenterX,
                    baselineY,
                    highlightLinePaint
                )
            }
        }

        // Draw X axis labels
        if (config.showAxisLabels) {
            for ((eIdx, entry) in data.withIndex()) {
                val rawCenterX = leftPadding + (eIdx * slotWidth) + (slotWidth / 2f)
                val slotCenterX = rawCenterX - config.panOffsetX
                if (slotCenterX in leftPadding..width - rightPadding) {
                    val rawText = entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                    val rotation = when {
                        config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                        rawText.length > 8 -> -90f
                        else -> 0f
                    }

                    val labelY = baselineY + 14f * density

                    if (rotation != 0f) {
                        canvas.withRotation(rotation, slotCenterX, labelY) {
                            drawText(
                                rawText,
                                slotCenterX,
                                labelY,
                                labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        }
                    } else {
                        val labelText =
                            if (rawText.length > config.xAxisLabelMaxLen) rawText.take(config.xAxisLabelMaxLen - 1) + "…" else rawText
                        canvas.drawText(
                            labelText,
                            slotCenterX,
                            labelY,
                            labelPaint.apply { textAlign = Paint.Align.CENTER })
                    }
                }
            }
        }

        // Draw Tooltip Badge AT THE ABSOLUTE FRONT OF EVERYTHING
        if (selectedStackedEntry != null && config.showTooltip) {
            val selectedEntry = selectedStackedEntry
            val totalVal = selectedEntry.values.sum()
            val headerLabel = selectedEntry.label ?: "Entry ${selectedEntry.x.toInt()}"
            val headerText = "$headerLabel: ${formatYValue(totalVal)}"

            val fullSegmentLines = mutableListOf<Pair<String, Int>>()
            for ((vIdx, valItem) in selectedEntry.values.withIndex()) {
                val segLabel =
                    selectedEntry.segmentLabels?.getOrNull(vIdx) ?: "Segment ${vIdx + 1}"
                val segColor =
                    selectedEntry.colors.getOrElse(vIdx) { config.style.highlightLineColor }
                fullSegmentLines.add(Pair("• $segLabel: ${formatYValue(valItem)}", segColor))
            }

            val headerTextSize = 12f * density
            val bodyTextSize = 10f * density
            val lineHeight = 18f * density
            val paddingX = config.tooltipPaddingXDp * density
            val paddingY = config.tooltipPaddingYDp * density

            // Constraint badge height to maximum chart height allowed
            val maxAllowedBadgeHeight = (height - 16f * density).coerceAtLeast(60f * density)
            val availableLineHeight =
                (maxAllowedBadgeHeight - (paddingY * 2f) - lineHeight).coerceAtLeast(lineHeight)
            val maxVisibleLines = (availableLineHeight / lineHeight).toInt().coerceAtLeast(1)

            val displayLines = if (fullSegmentLines.size > maxVisibleLines && maxVisibleLines > 1) {
                val visibleSub = fullSegmentLines.take(maxVisibleLines - 1).toMutableList()
                val hiddenCount = fullSegmentLines.size - (maxVisibleLines - 1)
                visibleSub.add(Pair("• +$hiddenCount more…", config.style.labelTextColor))
                visibleSub
            } else {
                fullSegmentLines
            }

            badgeTextPaint.textSize = headerTextSize
            badgeTextPaint.isFakeBoldText = true
            badgeTextPaint.getTextBounds(headerText, 0, headerText.length, textBoundsRect)
            var maxTextWidth = textBoundsRect.width().toFloat()

            badgeTextPaint.textSize = bodyTextSize
            badgeTextPaint.isFakeBoldText = false
            for (line in displayLines) {
                badgeTextPaint.getTextBounds(line.first, 0, line.first.length, textBoundsRect)
                if (textBoundsRect.width().toFloat() > maxTextWidth) {
                    maxTextWidth = textBoundsRect.width().toFloat()
                }
            }

            val badgeWidth = maxTextWidth + (paddingX * 2f)
            val badgeHeight =
                minOf((1 + displayLines.size) * lineHeight + (paddingY * 2f), maxAllowedBadgeHeight)

            val badgeX = selectedBarCenterX.coerceIn(
                leftPadding + badgeWidth / 2f,
                width - rightPadding - badgeWidth / 2f
            )
            val rawBadgeY = selectedBarTopY - 28f * density
            val badgeY = rawBadgeY.coerceIn(
                badgeHeight / 2f + 8f * density,
                height - badgeHeight / 2f - 8f * density
            )

            val badgeRect = RectF(
                badgeX - badgeWidth / 2f,
                badgeY - badgeHeight / 2f,
                badgeX + badgeWidth / 2f,
                badgeY + badgeHeight / 2f
            )

            val cornerPx = config.tooltipCornerRadiusDp * density
            glassBgPaint.color = config.style.tooltipBackgroundColor
            canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBgPaint)

            if (config.showTooltipBorder) {
                glassBorderPaint.color = config.style.tooltipBorderColor
                glassBorderPaint.strokeWidth = 1f * density
                canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBorderPaint)
            }

            val totalLines = 1 + displayLines.size
            val fontCapHeight = headerTextSize * 0.7f
            var currentTextY =
                badgeY - ((totalLines - 1) * lineHeight) / 2f + (fontCapHeight / 3.5f)

            badgeTextPaint.color = config.style.tooltipTextColor
            badgeTextPaint.textSize = headerTextSize
            badgeTextPaint.isFakeBoldText = true
            canvas.drawText(
                headerText,
                badgeX - (badgeWidth / 2f) + paddingX,
                currentTextY,
                badgeTextPaint
            )

            badgeTextPaint.textSize = bodyTextSize
            badgeTextPaint.isFakeBoldText = false
            for (line in displayLines) {
                currentTextY += lineHeight
                badgeTextPaint.color = line.second
                canvas.drawText(
                    line.first,
                    badgeX - (badgeWidth / 2f) + paddingX,
                    currentTextY,
                    badgeTextPaint
                )
            }
        }
    }
}
