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
 * Renderer for Radar/Spider web polygon charts.
 */
public class RadarRenderer : BaseRenderer<RadarDataSet> {

    private val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

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

        val labels = listOf("Speed", "Power", "Range", "Agility", "Stealth", "Armor")
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
            val ringPath = Path()
            for (i in 0 until axisCount) {
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val x = centerX + ringRadius * cos(angle)
                val y = centerY + ringRadius * sin(angle)
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            canvas.drawPath(ringPath, webPaint)
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
        for (dataset in data) {
            if (dataset.values.isEmpty()) continue

            val path = Path()
            val maxVal = 100f

            for (i in 0 until axisCount) {
                val valItem = dataset.values.getOrElse(i) { 0f }
                val currentRadius = radius * (valItem / maxVal) * progress
                val angle = i * angleStep - (Math.PI / 2.0).toFloat()
                val px = centerX + currentRadius * cos(angle)
                val py = centerY + currentRadius * sin(angle)

                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                canvas.drawCircle(px, py, 3f * density, strokePaint.apply { color = dataset.color; style = Paint.Style.FILL })
            }
            path.close()

            fillPaint.color = dataset.color
            fillPaint.alpha = (dataset.fillAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawPath(path, fillPaint)

            strokePaint.color = dataset.color
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f * density
            canvas.drawPath(path, strokePaint)
        }
    }
}
