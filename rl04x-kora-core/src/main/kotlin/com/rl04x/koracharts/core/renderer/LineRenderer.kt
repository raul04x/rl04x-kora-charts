package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale

/**
 * Renderer for Line Charts with Glassmorphic badge overlays.
 */
public class LineRenderer : BaseRenderer<Dataset> {

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

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#666666".toColorInt()
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val highlightLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#888888".toColorInt()
        strokeWidth = 2f
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

    // Glassmorphism brushes
    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
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
        badgeTextPaint.textSize = 8f * density

        val hasRotatedLabels =
            config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.firstOrNull()?.entries?.any {
                (it.label?.substringBefore('\n')?.length ?: 0) > 8
            } == true)
        val hasSecondaryY = config.showSecondaryYAxis || data.any { it.useSecondaryAxis }
        val hasLegend = config.showLegend && data.any { it.label.isNotEmpty() }

        val leftPadding =
            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val rightPadding =
            if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density + 10f * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 50f * density else 16f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding =
            config.paddingDp * density + (if (hasLegend) 22f * density else 12f * density)

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

        val visibleZoomX = maxOf(1f, config.zoomScaleX)
        val visibleRangeX = if (visibleZoomX == 1f) range.rangeX else range.rangeX / visibleZoomX
        val maxPanX = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
        val clampedPanX = config.panOffsetX.coerceIn(0f, maxPanX)
        val visibleMinX = range.minX + clampedPanX
        val visibleMaxX = visibleMinX + visibleRangeX

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) range.rangeY else range.rangeY / visibleZoomY
        val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMinY = range.minY + clampedPanY
        val visibleMaxY = visibleMinY + visibleRangeY

        val xTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = visibleMinX,
                max = visibleMaxX,
                targetTicks = 4,
                customStep = config.xAxisStep,
                forceInteger = config.forceIntegerTicks,
            )
        } else null

        val yTickResult = if (config.useNiceTicks) {
            com.rl04x.koracharts.core.engine.AxisTickCalculator.computeNiceTicks(
                min = visibleMinY,
                max = visibleMaxY,
                targetTicks = 4,
                customStep = config.yAxisStep,
                forceInteger = config.forceIntegerTicks,
                capAtMax = config.capNiceTicksAtMax,
            )
        } else null

        val effectiveMinX = xTickResult?.niceMin ?: visibleMinX
        val effectiveMaxX = xTickResult?.niceMax ?: visibleMaxX
        val effectiveRangeX = (effectiveMaxX - effectiveMinX).coerceAtLeast(1f)

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

        // Draw Reference / Threshold Lines
        for (refLine in config.referenceLines) {
            val pct = ((refLine.value - effectiveMinY) / effectiveRangeY).coerceIn(0f, 1f)
            val refY = topPadding + (1f - pct) * drawHeight
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
                        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
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

        // Draw grid
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
                if (xTickResult != null) {
                    for (xVal in xTickResult.ticks) {
                        val pct = (xVal - effectiveMinX) / effectiveRangeX
                        val x = leftPadding + pct * drawWidth
                        if (x in (leftPadding - 1f)..(width - rightPadding + 1f)) {
                            canvas.drawLine(x, topPadding, x, baselineY, gridPaint)
                        }
                    }
                } else {
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val x = leftPadding + (drawWidth * (i.toFloat() / gridSteps))
                        canvas.drawLine(x, topPadding, x, baselineY, gridPaint)
                    }
                }
            }
        }

        // Draw axes
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
                            capAtMax = config.capNiceTicksAtMax,
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

        val pendingBadges = mutableListOf<GlassBadgeItem>()

        // Min / Max Badges
        if (config.showMinMaxBadges) {
            for (dataset in data) {
                if (!dataset.visible || dataset.entries.isEmpty()) continue
                val maxEntry = dataset.entries.maxByOrNull { it.y }
                val minEntry = dataset.entries.minByOrNull { it.y }

                if (maxEntry != null) {
                    val sx =
                        leftPadding + ((maxEntry.x - effectiveMinX) / effectiveRangeX * drawWidth)
                    val sy =
                        baselineY - ((baselineY - (topPadding + (1f - (maxEntry.y - effectiveMinY) / effectiveRangeY) * drawHeight)) * progress)
                    pendingBadges.add(
                        GlassBadgeItem(
                            "▲ Max: ${formatYValue(maxEntry.y, null)}",
                            sx,
                            sy,
                            true
                        )
                    )
                }
                if (minEntry != null && minEntry != maxEntry) {
                    val sx =
                        leftPadding + ((minEntry.x - effectiveMinX) / effectiveRangeX * drawWidth)
                    val sy =
                        baselineY - ((baselineY - (topPadding + (1f - (minEntry.y - effectiveMinY) / effectiveRangeY) * drawHeight)) * progress)
                    pendingBadges.add(
                        GlassBadgeItem(
                            "▼ Min: ${formatYValue(minEntry.y, null)}",
                            sx,
                            sy,
                            false
                        )
                    )
                }
            }
        }

        // PASS 1: Plotting lines and node dots (clipped to chart area)
        canvas.withClip(
            leftPadding - 12f * density,
            topPadding - 16f * density,
            width - rightPadding + 16f * density,
            baselineY + 8f * density
        ) {
            for (dataset in data) {
                if (!dataset.visible || dataset.entries.isEmpty()) continue

                linePaint.color = dataset.color
                linePaint.strokeWidth = dataset.lineWidth * density
                pointPaint.color = dataset.color

                if (dataset.gradientFill) {
                    val startColor = dataset.gradientStartColor ?: dataset.color
                    val endColor = dataset.gradientEndColor ?: Color.TRANSPARENT
                    val alphaInt = (dataset.fillAlpha * 255 * progress).toInt().coerceIn(0, 255)
                    val colorWithAlpha = (startColor and 0x00FFFFFF) or (alphaInt shl 24)

                    fillPaint.shader = LinearGradient(
                        0f, topPadding, 0f, baselineY,
                        colorWithAlpha, endColor,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    fillPaint.shader = null
                    fillPaint.color = dataset.color
                    fillPaint.alpha = (dataset.fillAlpha * 255 * progress).toInt().coerceIn(0, 255)
                }

                val path = Path()
                val fillPath = Path()

                val points = dataset.entries.map { entry ->
                    val sx = leftPadding + (if (effectiveRangeX == 0f) {
                        drawWidth / 2f
                    } else {
                        (entry.x - effectiveMinX) / effectiveRangeX * drawWidth
                    })

                    val targetY = topPadding + (if (effectiveRangeY == 0f) {
                        drawHeight / 2f
                    } else {
                        (1f - (entry.y - effectiveMinY) / effectiveRangeY) * drawHeight
                    })
                    val sy = baselineY - ((baselineY - targetY) * progress)
                    Pair(sx, sy)
                }

                val firstPoint = points.first()
                path.moveTo(firstPoint.first, firstPoint.second)
                fillPath.moveTo(firstPoint.first, baselineY)
                fillPath.lineTo(firstPoint.first, firstPoint.second)

                if (dataset.isCurved && points.size > 2) {
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlX1 = p1.first + (p2.first - p1.first) / 2f
                        val controlY1 = p1.second
                        val controlX2 = p1.first + (p2.first - p1.first) / 2f
                        val controlY2 = p2.second

                        path.cubicTo(
                            controlX1,
                            controlY1,
                            controlX2,
                            controlY2,
                            p2.first,
                            p2.second
                        )
                        fillPath.cubicTo(
                            controlX1,
                            controlY1,
                            controlX2,
                            controlY2,
                            p2.first,
                            p2.second
                        )
                    }
                } else {
                    for (i in 1 until points.size) {
                        val p = points[i]
                        path.lineTo(p.first, p.second)
                        fillPath.lineTo(p.first, p.second)
                    }
                }

                val lastPoint = points.last()
                fillPath.lineTo(lastPoint.first, baselineY)
                fillPath.close()

                canvas.drawPath(fillPath, fillPaint)
                canvas.drawPath(path, linePaint)

                // Draw node points
                if (dataset.showPoints) {
                    for ((idx, p) in points.withIndex()) {
                        val entry = dataset.entries[idx]
                        val isSelected = (config.selectedEntry == entry)
                        val radius =
                            if (isSelected) dataset.pointRadius * density * 1.8f else dataset.pointRadius * density
                        canvas.drawCircle(p.first, p.second, radius, pointPaint)

                        if ((config.showPointValues || isSelected) && progress >= 0.5f) {
                            val valueText = config.pointValueFormatter?.invoke(entry)
                                ?: entry.label
                                ?: formatYValue(entry.y, config.yAxisFormatter)

                            pendingBadges.add(
                                GlassBadgeItem(
                                    valueText,
                                    p.first,
                                    p.second,
                                    isSelected
                                )
                            )
                        }
                    }
                }
            }

            // Draw highlight lines for selected point
            if (config.selectedEntry != null && config.showHighlightLine) {
                val sel = config.selectedEntry
                val sx = leftPadding + (if (effectiveRangeX == 0f) {
                    drawWidth / 2f
                } else {
                    (sel.x - effectiveMinX) / effectiveRangeX * drawWidth
                })
                val targetY = topPadding + (if (effectiveRangeY == 0f) {
                    drawHeight / 2f
                } else {
                    (1f - (sel.y - effectiveMinY) / effectiveRangeY) * drawHeight
                })
                val sy = baselineY - ((baselineY - targetY) * progress)

                if (config.showHighlightLineX) {
                    canvas.drawLine(sx, topPadding, sx, baselineY, highlightLinePaint)
                }
                if (config.showHighlightLineY) {
                    canvas.drawLine(leftPadding, sy, width - rightPadding, sy, highlightLinePaint)
                }

                highlightPointInnerPaint.color = data.firstOrNull()?.color ?: Color.BLUE
                canvas.drawCircle(sx, sy, 8f * density, highlightPointOuterPaint)
                canvas.drawCircle(sx, sy, 5f * density, highlightPointInnerPaint)
            }
        }

        // PASS 2: Draw X axis labels in unclipped space below baseline
        if (config.showAxisLabels) {
            val firstDataset = data.firstOrNull { it.visible && it.entries.isNotEmpty() }
            if (firstDataset != null) {
                if (xTickResult != null) {
                    for (xVal in xTickResult.ticks) {
                        val pct = (xVal - effectiveMinX) / effectiveRangeX
                        val sx = leftPadding + pct * drawWidth

                        if (sx in (leftPadding - 1f)..(width - rightPadding + 1f)) {
                            val matchedEntry =
                                firstDataset.entries.firstOrNull { kotlin.math.abs(it.x - xVal) < 0.001f }
                            val rawText = config.xAxisFormatter?.invoke(xVal)
                                ?: matchedEntry?.label?.substringBefore('\n')
                                ?: if (config.forceIntegerTicks) String.format(
                                    Locale.US,
                                    "%.0f",
                                    xVal
                                ) else String.format(Locale.US, "%.1f", xVal)

                            val rotation = when {
                                config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                                rawText.length > 8 -> -90f
                                else -> 0f
                            }

                            val labelY = baselineY + 10f * density

                            if (rotation != 0f) {
                                canvas.withRotation(rotation, sx, labelY) {
                                    drawText(
                                        rawText,
                                        sx,
                                        labelY,
                                        labelPaint.apply { textAlign = Paint.Align.RIGHT })
                                }
                            } else {
                                val labelText =
                                    if (rawText.length > config.xAxisLabelMaxLen) rawText.take(
                                        config.xAxisLabelMaxLen - 1
                                    ) + "…" else rawText
                                canvas.drawText(
                                    labelText,
                                    sx,
                                    labelY,
                                    labelPaint.apply { textAlign = Paint.Align.CENTER })
                            }
                        }
                    }
                } else {
                    for (entry in firstDataset.entries) {
                        val sx = leftPadding + (if (range.rangeX == 0f) {
                            drawWidth / 2f
                        } else {
                            val visibleZoomX = maxOf(1f, config.zoomScaleX)
                            val visibleRangeX = range.rangeX / visibleZoomX
                            val maxPanX = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
                            val clampedPanX = config.panOffsetX.coerceIn(0f, maxPanX)
                            val visibleMinX = range.minX + clampedPanX
                            (entry.x - visibleMinX) / visibleRangeX * drawWidth
                        })

                        if (sx in leftPadding..width - rightPadding) {
                            val rawText = config.xAxisFormatter?.invoke(entry.x)
                                ?: entry.label?.substringBefore('\n')
                                ?: String.format(Locale.US, "%.0f", entry.x)
                            val rotation = when {
                                config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                                rawText.length > 8 -> -90f
                                else -> 0f
                            }

                            val labelY = baselineY + 10f * density

                            if (rotation != 0f) {
                                canvas.withRotation(rotation, sx, labelY) {
                                    drawText(
                                        rawText,
                                        sx,
                                        labelY,
                                        labelPaint.apply { textAlign = Paint.Align.RIGHT })
                                }
                            } else {
                                val labelText =
                                    if (rawText.length > config.xAxisLabelMaxLen) rawText.take(
                                        config.xAxisLabelMaxLen - 1
                                    ) + "…" else rawText
                                canvas.drawText(
                                    labelText,
                                    sx,
                                    labelY,
                                    labelPaint.apply { textAlign = Paint.Align.CENTER })
                            }
                        }
                    }
                }
            }
        }

        // PASS 3: Draw Glassmorphic Badges on top of everything
        val cornerPx = config.tooltipCornerRadiusDp * density
        for (badge in pendingBadges) {
            val lines = badge.text.split("\n")
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

            val cx = badge.x.coerceIn(
                leftPadding + badgeWidth / 2f,
                width - rightPadding - badgeWidth / 2f
            )
            val cy = (badge.y - 28f * density).coerceAtLeast(topPadding + badgeHeight / 2f)

            val badgeRect = RectF(
                cx - (badgeWidth / 2f),
                cy - (badgeHeight / 2f),
                cx + (badgeWidth / 2f),
                cy + (badgeHeight / 2f),
            )

            glassBgPaint.color = config.style.tooltipBackgroundColor
            canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBgPaint)

            if (config.showTooltipBorder) {
                glassBorderPaint.strokeWidth = 1f * density
                glassBorderPaint.color = config.style.tooltipBorderColor
                canvas.drawRoundRect(badgeRect, cornerPx, cornerPx, glassBorderPaint)
            }

            val totalLines = lines.size
            val fontCapHeight = 12f * density * 0.7f
            var startY = cy - ((totalLines - 1) * lineSpacing) / 2f + (fontCapHeight / 3.5f)
            for (line in lines) {
                canvas.drawText(line, cx, startY, badgeTextPaint)
                startY += lineSpacing
            }
        }
    }
}
