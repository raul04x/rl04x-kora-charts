package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale

/**
 * Renderer for Horizontal Bar Charts with track background and value labels.
 */
public class HorizontalBarRenderer : BaseRenderer<Dataset> {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        textAlign = Paint.Align.LEFT
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        textAlign = Paint.Align.RIGHT
    }

    private data class HorizontalItem(
        val label: String,
        val value: Float,
        val color: Int,
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

        val density = Resources.getSystem().displayMetrics.density
        labelPaint.textSize = 11f * density
        valuePaint.textSize = 11f * density

        trackPaint.color = config.style.trackBackgroundColor
        labelPaint.color = config.style.labelTextColor
        valuePaint.color = config.style.labelTextColor

        // Extract items from multiple Datasets or a single Dataset with multiple entries
        val items = mutableListOf<HorizontalItem>()
        if (data.size > 1) {
            for (dataset in data) {
                val entry = dataset.entries.firstOrNull() ?: continue
                items.add(HorizontalItem(entry.label ?: dataset.label, entry.y, dataset.color))
            }
        } else if (data.isNotEmpty()) {
            val dataset = data.first()
            for (entry in dataset.entries) {
                items.add(HorizontalItem(entry.label ?: dataset.label, entry.y, dataset.color))
            }
        }

        if (items.isEmpty()) return

        val leftMargin = 90f * density
        val rightMargin = 48f * density
        val topPadding = config.paddingDp * density + 8f * density
        val bottomPadding = config.paddingDp * density
        val drawWidth = width - leftMargin - rightMargin
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val itemCount = items.size
        val slotHeight = drawHeight / itemCount
        val barHeight = (slotHeight * 0.45f).coerceIn(6f * density, 20f * density)
        val maxY = items.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 100f

        for ((idx, item) in items.withIndex()) {
            val centerY = topPadding + (idx * slotHeight) + (slotHeight / 2f)
            val barTop = centerY - (barHeight / 2f)
            val barBottom = centerY + (barHeight / 2f)

            // Category label on left
            canvas.drawText(item.label, 8f * density, centerY + 4f * density, labelPaint)

            // Background track
            val trackRect = RectF(leftMargin, barTop, leftMargin + drawWidth, barBottom)
            canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, trackPaint)

            // Animated progress bar
            val fillWidth = (item.value / maxY) * drawWidth * progress
            if (fillWidth > 0f) {
                fillPaint.color = item.color
                val fillRect = RectF(leftMargin, barTop, leftMargin + fillWidth, barBottom)
                canvas.drawRoundRect(fillRect, barHeight / 2f, barHeight / 2f, fillPaint)
            }

            // Value text on right (ms)
            val valueText = String.format(Locale.US, "%.0fms", item.value)
            canvas.drawText(valueText, width - 8f * density, centerY + 4f * density, valuePaint)
        }
    }
}
