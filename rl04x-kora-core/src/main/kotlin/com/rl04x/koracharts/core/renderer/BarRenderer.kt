package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale

/**
 * Renderer for Bar Charts with Glassmorphic badge overlays.
 */
public class BarRenderer : BaseRenderer<Dataset> {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E0E0E0".toColorInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#888888".toColorInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#666666".toColorInt()
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val highlightBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#888888".toColorInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    // Glassmorphism brushes
    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val glassShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val textBoundsRect = Rect()

    private data class GlassBadgeItem(
        val text: String,
        val x: Float,
        val y: Float,
        val isSelected: Boolean,
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

        gridPaint.color = config.style.gridColor
        axisPaint.color = config.style.axisColor
        labelPaint.color = config.style.labelTextColor
        highlightLinePaint.color = config.style.highlightLineColor
        badgeTextPaint.color = config.style.tooltipTextColor

        val density = Resources.getSystem().displayMetrics.density
        labelPaint.textSize = 10f * density
        badgeTextPaint.textSize = 10f * density

        val hasRotatedLabels =
            config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.firstOrNull()?.entries?.any {
                (it.label?.length ?: 0) > 8
            } == true)
        val hasSecondaryY = config.showSecondaryYAxis || data.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && data.any { it.label.isNotEmpty() }

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding =
            config.paddingDp * density + (if (hasLegend) 24f * density else 16f * density)

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        fun formatYValue(value: Float, customFormatter: ((Float) -> String)?): String {
            if (customFormatter != null) return customFormatter.invoke(value)
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

        val engine = ChartEngine(width, height, config.paddingDp * density)
        val range = engine.computeRange(data)

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) range.rangeY else range.rangeY / visibleZoomY
        val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMinY = range.minY + clampedPanY
        val visibleMaxY = visibleMinY + visibleRangeY

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = visibleMinY,
                max = visibleMaxY,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
            )
        } else null

        val effectiveMinY = yTickResult?.niceMin ?: visibleMinY
        val effectiveMaxY = yTickResult?.niceMax ?: visibleMaxY
        val effectiveRangeY = (effectiveMaxY - effectiveMinY).coerceAtLeast(1f)

        val baselineY = topPadding + drawHeight

        // Draw Target Zones
        for (zone in config.targetZones) {
            val topPct = ((zone.maxY - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val bottomPct = ((zone.minY - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
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
            val pct = ((refLine.value - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val refY = topPadding + (1f - pct) * drawHeight
            if (refY in topPadding..baselineY) {
                val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = refLine.color
                    strokeWidth = refLine.strokeWidthDp * density
                    style = Paint.Style.STROKE
                    if (refLine.isDashed) {
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
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
        if (config.showAverageLine) {
            val allVisibleEntries = data.filter { it.visible }.flatMap { it.entries }
            if (allVisibleEntries.isNotEmpty()) {
                val avgY = allVisibleEntries.map { it.y }.average().toFloat()
                val pct = ((avgY - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
                val avgScreenY = topPadding + (1f - pct) * drawHeight
                if (avgScreenY in topPadding..baselineY) {
                    val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = config.averageLineColor ?: config.style.highlightLineColor
                        strokeWidth = 1.5f * density
                        style = Paint.Style.STROKE
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
                    }
                    canvas.drawLine(
                        leftPadding,
                        avgScreenY,
                        width - rightPadding,
                        avgScreenY,
                        avgPaint
                    )

                    val avgTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = config.averageLineColor ?: config.style.highlightLineColor
                        textSize = 9f * density
                        textAlign = Paint.Align.LEFT
                    }
                    val avgText = "Avg: ${formatYValue(avgY, config.yAxisFormatter)}"
                    canvas.drawText(
                        avgText,
                        leftPadding + 6f * density,
                        avgScreenY - 4f * density,
                        avgTextPaint
                    )
                }
            }
        }

        // Draw Legend at the top if enabled and dataset labels exist
        if (hasLegend) {
            val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.style.labelTextColor
                textSize = 10f * density
                textAlign = Paint.Align.LEFT
            }
            val legendDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            var currentX = leftPadding
            val legendY = config.paddingDp * density + 11f * density

            for (dataset in data) {
                if (dataset.label.isEmpty() || !dataset.visible) continue

                legendDotPaint.color = dataset.color
                canvas.drawCircle(
                    currentX + 4f * density,
                    legendY - 3f * density,
                    4f * density,
                    legendDotPaint
                )

                canvas.drawText(dataset.label, currentX + 11f * density, legendY, legendTextPaint)

                val labelWidth = legendTextPaint.measureText(dataset.label)
                currentX += 11f * density + labelWidth + 16f * density
                if (currentX > width - rightPadding) break
            }
        }

        if (config.showGrid) {
            if (yTickResult != null) {
                for (yVal in yTickResult.ticks) {
                    val pct = (yVal - effectiveMinY) / effectiveRangeY
                    val y = topPadding + (1f - pct) * drawHeight
                    if (y in (topPadding - 1f)..(baselineY + 1f)) {
                        canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
                        if (config.showAxisLabels) {
                            val labelText = formatYValue(yVal, config.yAxisFormatter)
                            canvas.drawText(
                                labelText,
                                leftPadding - 8f * density,
                                y + 4f * density,
                                labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        }
                    }
                }
            } else {
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = topPadding + (drawHeight * (i.toFloat() / gridSteps))
                    canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)

                    if (config.showAxisLabels) {
                        val yVal = visibleMaxY - (visibleRangeY * (i.toFloat() / gridSteps))
                        val labelText = formatYValue(yVal, config.yAxisFormatter)
                        canvas.drawText(
                            labelText,
                            leftPadding - 8f * density,
                            y + 4f * density,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                    }
                }
            }

            if (config.showVerticalGrid) {
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val x = leftPadding + (drawWidth * (i.toFloat() / gridSteps))
                    canvas.drawLine(x, topPadding, x, baselineY, gridPaint)
                }
            }
        }

        if (config.showAxes) {
            canvas.drawLine(leftPadding, baselineY, width - rightPadding, baselineY, axisPaint)
            canvas.drawLine(leftPadding, topPadding, leftPadding, baselineY, axisPaint)

            if (hasSecondaryY) {
                canvas.drawLine(
                    width - rightPadding,
                    topPadding,
                    width - rightPadding,
                    baselineY,
                    axisPaint
                )

                val secDatasets =
                    data.filter { it.useSecondaryAxis && it.visible && it.entries.isNotEmpty() }
                if (secDatasets.isNotEmpty()) {
                    val secRange = engine.computeRange(secDatasets)
                    val secYTickResult = if (config.useNiceTicks) {
                        com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                            min = secRange.minY,
                            max = secRange.maxY,
                            targetTicks = 4,
                            customStep = config.yAxisStep,
                            forceInteger = config.forceIntegerTicks,
                        )
                    } else null

                    val secMinY = secYTickResult?.niceMin ?: secRange.minY
                    val secMaxY = secYTickResult?.niceMax ?: secRange.maxY
                    val secRangeY = (secMaxY - secMinY).coerceAtLeast(1f)

                    val secTicks =
                        secYTickResult?.ticks ?: listOf(secMinY, secMinY + secRangeY / 2f, secMaxY)
                    for (yVal in secTicks) {
                        val pct = (yVal - secMinY) / secRangeY
                        val y = topPadding + (1f - pct) * drawHeight
                        if (y in (topPadding - 1f)..(baselineY + 1f)) {
                            val labelText = formatYValue(yVal, config.secondaryYAxisFormatter)
                            canvas.drawText(
                                labelText,
                                width - rightPadding + 6f * density,
                                y + 4f * density,
                                labelPaint.apply { textAlign = Paint.Align.LEFT })
                        }
                    }
                }
            }
        }

        val visibleDatasets = data.filter { it.visible && it.entries.isNotEmpty() }
        if (visibleDatasets.isEmpty()) return

        val firstDataset = visibleDatasets.first()
        val entryCount = firstDataset.entries.size
        val effectiveZoomX = maxOf(1f, config.zoomScaleX)
        val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
        val barWidth = scaledSlotWidth * 0.6f / visibleDatasets.size

        val maxPanPixels = (drawWidth * effectiveZoomX - drawWidth).coerceAtLeast(0f)
        val clampedPanPixels = config.panOffsetX.coerceIn(0f, maxPanPixels)

        val pendingBadges = mutableListOf<GlassBadgeItem>()

        // PASS 1: Drawing bars clipped to plotting area
        canvas.withClip(
            leftPadding,
            topPadding - 4f * density,
            width - rightPadding,
            baselineY + 2f * density
        ) {
            for (eIdx in 0 until entryCount) {
                val slotLeft = leftPadding + (eIdx * scaledSlotWidth) - clampedPanPixels

                for ((dIdx, dataset) in visibleDatasets.withIndex()) {
                    if (eIdx >= dataset.entries.size) continue
                    val entry = dataset.entries[eIdx]
                    val barLeft =
                        slotLeft + ((scaledSlotWidth - (barWidth * visibleDatasets.size)) / 2f) + (dIdx * barWidth)
                    val barRight = barLeft + barWidth

                    val targetY = topPadding + (if (effectiveRangeY == 0f) {
                        drawHeight / 2f
                    } else {
                        (1f - (entry.y - effectiveMinY) / effectiveRangeY) * drawHeight
                    })
                    val animatedY = baselineY - ((baselineY - targetY) * progress)

                    barPaint.color = dataset.color
                    val rect = RectF(barLeft, animatedY, barRight, baselineY)
                    canvas.drawRoundRect(rect, 8f, 8f, barPaint)

                    val isSelected = (config.selectedEntry == entry)
                    if (isSelected) {
                        canvas.drawRoundRect(rect, 8f, 8f, highlightBorderPaint)
                        if (config.showHighlightLine) {
                            val cx = barLeft + (barWidth / 2f)
                            if (config.showHighlightLineY) {
                                canvas.drawLine(
                                    leftPadding,
                                    animatedY,
                                    width - rightPadding,
                                    animatedY,
                                    highlightLinePaint
                                )
                            }
                            if (config.showHighlightLineX) {
                                canvas.drawLine(cx, topPadding, cx, baselineY, highlightLinePaint)
                            }
                        }
                    }

                    if ((config.showPointValues || isSelected) && progress >= 0.5f) {
                        val valueText = config.pointValueFormatter?.invoke(entry)
                            ?: String.format(Locale.US, "%.1f", entry.y)

                        val cx = barLeft + (barWidth / 2f)
                        val cy = animatedY - 22f * density

                        pendingBadges.add(GlassBadgeItem(valueText, cx, cy, isSelected))
                    }
                }
            }
        }

        // PASS 2: Drawing X axis labels in unclipped space below baseline
        if (config.showAxisLabels) {
            for (eIdx in 0 until entryCount) {
                val slotLeft = leftPadding + (eIdx * scaledSlotWidth) - clampedPanPixels
                val slotCenterX = slotLeft + (scaledSlotWidth / 2f)

                if (slotCenterX in leftPadding..width - rightPadding && firstDataset.entries.size > eIdx) {
                    val entry = firstDataset.entries[eIdx]
                    val rawText =
                        config.xAxisFormatter?.invoke(entry.x) ?: entry.label ?: String.format(
                            Locale.US,
                            "%.0f",
                            entry.x
                        )
                    val rotation = when {
                        config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                        rawText.length > 8 -> -90f
                        else -> 0f
                    }

                    val labelX = slotCenterX
                    val labelY = baselineY + 10f * density

                    if (rotation != 0f) {
                        canvas.save()
                        canvas.rotate(rotation, labelX, labelY)
                        canvas.drawText(
                            rawText,
                            labelX,
                            labelY,
                            labelPaint.apply { textAlign = Paint.Align.RIGHT })
                        canvas.restore()
                    } else {
                        val labelText =
                            if (rawText.length > config.xAxisLabelMaxLen) rawText.take(config.xAxisLabelMaxLen - 1) + "…" else rawText
                        canvas.drawText(
                            labelText,
                            labelX,
                            labelY,
                            labelPaint.apply { textAlign = Paint.Align.CENTER })
                    }
                }
            }
        }

        // PASS 3: Draw Glassmorphic Badges on top of everything
        for (badge in pendingBadges) {
            badgeTextPaint.getTextBounds(badge.text, 0, badge.text.length, textBoundsRect)
            val textWidth = badgeTextPaint.measureText(badge.text)
            val textHeight = textBoundsRect.height().toFloat()

            val paddingX = 7f * density
            val paddingY = 4f * density

            val badgeWidth = textWidth + (paddingX * 2f)
            val badgeHeight = textHeight + (paddingY * 2f)

            val badgeRect = RectF(
                badge.x - (badgeWidth / 2f),
                badge.y - (badgeHeight / 2f),
                badge.x + (badgeWidth / 2f),
                badge.y + (badgeHeight / 2f),
            )

            val shadowRect = RectF(
                badgeRect.left,
                badgeRect.top + 2f * density,
                badgeRect.right,
                badgeRect.bottom + 2f * density
            )
            glassShadowPaint.color =
                Color.argb((0.25f * 255 * progress).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawRoundRect(shadowRect, 7f * density, 7f * density, glassShadowPaint)

            val bgAlpha =
                ((if (badge.isSelected) 0.92f else 0.82f) * 255 * progress).toInt().coerceIn(0, 255)
            glassBgPaint.color = Color.argb(bgAlpha, 22, 22, 26)

            glassBorderPaint.strokeWidth = 1.2f * density
            val borderAlpha =
                ((if (badge.isSelected) 0.65f else 0.40f) * 255 * progress).toInt().coerceIn(0, 255)
            glassBorderPaint.color = Color.argb(borderAlpha, 255, 255, 255)

            canvas.drawRoundRect(badgeRect, 7f * density, 7f * density, glassBgPaint)
            canvas.drawRoundRect(badgeRect, 7f * density, 7f * density, glassBorderPaint)

            val textY = badge.y + (textHeight / 2f) - 1.5f * density
            canvas.drawText(badge.text, badge.x, textY, badgeTextPaint)
        }
    }
}
