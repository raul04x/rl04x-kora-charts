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

        val hasRotatedLabels = config.showAxisLabels && (config.xAxisLabelRotation != 0f || data.firstOrNull()?.entries?.any { (it.label?.length ?: 0) > 8 } == true)

        val leftPadding = if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
        val bottomPadding = if (config.showAxisLabels) {
            config.paddingDp * density + (if (hasRotatedLabels) 60f * density else 20f * density)
        } else {
            config.paddingDp * density
        }
        val topPadding = config.paddingDp * density + 16f * density
        val rightPadding = config.paddingDp * density

        val drawWidth = width - leftPadding - rightPadding
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val engine = ChartEngine(width, height, config.paddingDp * density)
        val range = engine.computeRange(data)

        val visibleZoomY = maxOf(1f, config.zoomScaleY)
        val visibleRangeY = if (visibleZoomY == 1f) range.rangeY else range.rangeY / visibleZoomY
        val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
        val clampedPanY = config.panOffsetY.coerceIn(0f, maxPanY)
        val visibleMinY = range.minY + clampedPanY
        val visibleMaxY = visibleMinY + visibleRangeY

        val baselineY = topPadding + drawHeight

        // Draw grid
        if (config.showGrid) {
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = topPadding + (drawHeight * (i.toFloat() / gridSteps))
                canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)

                if (config.showAxisLabels) {
                    val yVal = visibleMaxY - (visibleRangeY * (i.toFloat() / gridSteps))
                    val labelText = config.yAxisFormatter?.invoke(yVal) ?: String.format(Locale.US, "%.1f", yVal)
                    canvas.drawText(labelText, leftPadding - 8f * density, y + 4f * density, labelPaint.apply { textAlign = Paint.Align.RIGHT })
                }
            }

            if (config.showVerticalGrid) {
                for (i in 0..gridSteps) {
                    val x = leftPadding + (drawWidth * (i.toFloat() / gridSteps))
                    canvas.drawLine(x, topPadding, x, baselineY, gridPaint)
                }
            }
        }

        // Draw axes
        if (config.showAxes) {
            canvas.drawLine(leftPadding, baselineY, width - rightPadding, baselineY, axisPaint)
            canvas.drawLine(leftPadding, topPadding, leftPadding, baselineY, axisPaint)
        }

        val pendingBadges = mutableListOf<GlassBadgeItem>()

        // PASS 1: Plotting lines and node dots (clipped to chart area)
        canvas.withClip(leftPadding, topPadding - 4f * density, width - rightPadding, baselineY + 2f * density) {
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

                    val targetY = topPadding + (if (visibleRangeY == 0f) {
                        drawHeight / 2f
                    } else {
                        (1f - (entry.y - visibleMinY) / visibleRangeY) * drawHeight
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

                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.first, p2.second)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.first, p2.second)
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
                        val radius = if (isSelected) dataset.pointRadius * density * 1.8f else dataset.pointRadius * density
                        canvas.drawCircle(p.first, p.second, radius, pointPaint)

                        if ((config.showPointValues || isSelected) && progress >= 0.5f) {
                            val valueText = config.pointValueFormatter?.invoke(entry)
                                ?: (entry.label?.let { "$it: ${entry.y}" } ?: String.format(Locale.US, "%.1f", entry.y))

                            pendingBadges.add(GlassBadgeItem(valueText, p.first, p.second, isSelected))
                        }
                    }
                }
            }

            // Draw highlight lines for selected point
            if (config.selectedEntry != null && config.showHighlightLine) {
                val sel = config.selectedEntry
                val sx = leftPadding + (if (range.rangeX == 0f) {
                    drawWidth / 2f
                } else {
                    val visibleZoomX = maxOf(1f, config.zoomScaleX)
                    val visibleRangeX = range.rangeX / visibleZoomX
                    val maxPanX = (range.rangeX - visibleRangeX).coerceAtLeast(0f)
                    val clampedPanX = config.panOffsetX.coerceIn(0f, maxPanX)
                    val visibleMinX = range.minX + clampedPanX
                    (sel.x - visibleMinX) / visibleRangeX * drawWidth
                })
                val targetY = topPadding + (if (visibleRangeY == 0f) {
                    drawHeight / 2f
                } else {
                    (1f - (sel.y - visibleMinY) / visibleRangeY) * drawHeight
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
                        val rawText = config.xAxisFormatter?.invoke(entry.x) ?: entry.label ?: String.format(Locale.US, "%.0f", entry.x)
                        val rotation = when {
                            config.xAxisLabelRotation != 0f -> config.xAxisLabelRotation
                            rawText.length > 8 -> -90f
                            else -> 0f
                        }

                        val labelX = sx
                        val labelY = baselineY + 10f * density

                        if (rotation != 0f) {
                            canvas.save()
                            canvas.rotate(rotation, labelX, labelY)
                            canvas.drawText(rawText, labelX, labelY, labelPaint.apply { textAlign = Paint.Align.RIGHT })
                            canvas.restore()
                        } else {
                            val labelText = if (rawText.length > config.xAxisLabelMaxLen) rawText.take(config.xAxisLabelMaxLen - 1) + "…" else rawText
                            canvas.drawText(labelText, labelX, labelY, labelPaint.apply { textAlign = Paint.Align.CENTER })
                        }
                    }
                }
            }
        }

        // PASS 3: Draw Glassmorphic Badges on top of everything
        for (badge in pendingBadges) {
            badgeTextPaint.getTextBounds(badge.text, 0, badge.text.length, textBoundsRect)
            val textWidth = badgeTextPaint.measureText(badge.text)
            val textHeight = textBoundsRect.height().toFloat()

            val paddingX = 8f * density
            val paddingY = 5f * density

            val badgeWidth = textWidth + (paddingX * 2f)
            val badgeHeight = textHeight + (paddingY * 2f)

            val cx = badge.x
            val cy = badge.y - 24f * density

            val badgeRect = RectF(
                cx - (badgeWidth / 2f),
                cy - (badgeHeight / 2f),
                cx + (badgeWidth / 2f),
                cy + (badgeHeight / 2f),
            )

            val shadowRect = RectF(badgeRect.left, badgeRect.top + 2f * density, badgeRect.right, badgeRect.bottom + 2f * density)
            glassShadowPaint.color = Color.argb((0.25f * 255 * progress).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawRoundRect(shadowRect, 7f * density, 7f * density, glassShadowPaint)

            glassBgPaint.color = config.style.tooltipBackgroundColor
            glassBorderPaint.strokeWidth = 1.2f * density
            glassBorderPaint.color = config.style.tooltipBorderColor

            canvas.drawRoundRect(badgeRect, 7f * density, 7f * density, glassBgPaint)
            canvas.drawRoundRect(badgeRect, 7f * density, 7f * density, glassBorderPaint)

            val textY = cy + (textHeight / 2f) - 1.5f * density
            canvas.drawText(badge.text, cx, textY, badgeTextPaint)
        }
    }
}
