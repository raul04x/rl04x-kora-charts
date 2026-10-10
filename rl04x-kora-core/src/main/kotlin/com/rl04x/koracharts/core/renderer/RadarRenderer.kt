package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.RadarDataSet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderer for Radar/Spider web polygon charts with zero allocations in draw().
 */
public class RadarRenderer : BaseRenderer<RadarDataSet> {

    private val webPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f; style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; style = Paint.Style.STROKE }
    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; textAlign = Paint.Align.CENTER }

    private val cachedPath = Path()
    private val cachedRingPath = Path()

    private val labels = listOf("Speed", "Power", "Range", "Agility", "Stealth", "Armor")

    override fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<RadarDataSet>,
        config: ChartConfig,
        progress: Float,
    ) {
        if (data.isEmpty() || width <= 0f || height <= 0f) return

        val density = Resources.getSystem().displayMetrics.density
        webPaint.color = config.style.gridColor
        labelPaint.color = config.style.labelTextColor
        labelPaint.textSize = 10f * density

        val axisCount = labels.size
        val centerX = width / 2f
        val centerY = height / 2f + 4f * density
        val radius = (minOf(width, height) / 2f) - (48f * density)
        if (radius <= 0f) return

        val angleStep = (2.0 * Math.PI / axisCount).toFloat()

        // Draw concentric web rings
        val ringCount = 4
        for (r in 1..ringCount) {
            val ringRadius = radius * (r.toFloat() / ringCount)
            cachedRingPath.reset()
            for (i in 0 until axisCount) {
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val x = centerX + ringRadius * cos(angle)
                val y = centerY + ringRadius * sin(angle)
                if (i == 0) cachedRingPath.moveTo(x, y) else cachedRingPath.lineTo(x, y)
            }
            cachedRingPath.close()
            canvas.drawPath(cachedRingPath, webPaint)
        }

        // Draw radial axes and category labels
        for (i in 0 until axisCount) {
            val angle = i * angleStep - (Math.PI / 2.0).toFloat()
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)
            canvas.drawLine(centerX, centerY, endX, endY, webPaint)

            val labelX = centerX + (radius + 18f * density) * cos(angle)
            val labelY = centerY + (radius + 18f * density) * sin(angle) + 4f * density
            canvas.drawText(labels[i], labelX, labelY, labelPaint)
        }

        // Draw dataset polygons
        for ((_, values, color, fillAlpha) in data) {
            if (values.isEmpty()) continue

            cachedPath.reset()
            val maxVal = 100f

            for (i in 0 until axisCount) {
                val valItem = values.getOrElse(i) { 0f }
                val currentRadius = radius * (valItem / maxVal) * progress
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val px = centerX + currentRadius * cos(angle)
                val py = centerY + currentRadius * sin(angle)

                if (i == 0) cachedPath.moveTo(px, py) else cachedPath.lineTo(px, py)
                strokePaint.style = Paint.Style.FILL
                strokePaint.color = color
                canvas.drawCircle(px, py, 3f * density, strokePaint)
            }
            cachedPath.close()

            fillPaint.color = color
            fillPaint.alpha = (fillAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawPath(cachedPath, fillPaint)

            strokePaint.color = color
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f * density
            canvas.drawPath(cachedPath, strokePaint)
        }
    }
}
