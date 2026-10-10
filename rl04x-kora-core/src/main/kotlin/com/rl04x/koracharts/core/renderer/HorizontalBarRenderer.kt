package com.rl04x.koracharts.core.renderer

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import java.util.Locale

/**
 * Renderer for Horizontal Bar Charts with track background and value labels.
 */
public class HorizontalBarRenderer : BaseRenderer<Dataset> {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; textAlign = Paint.Align.LEFT }
    private val valuePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; textAlign = Paint.Align.RIGHT }

    private val cachedTrackRect = RectF()
    private val cachedFillRect = RectF()

    private class HorizontalItem(
        var label: String = "",
        var value: Float = 0f,
        var color: Int = 0,
    )

    private val itemPool =
        ArrayList<HorizontalItem>(32).apply { repeat(32) { add(HorizontalItem()) } }
    private var itemCount = 0

    private val defaultColors = listOf(
        "#F87171".toColorInt(), // Coral (Redis)
        "#F59E0B".toColorInt(), // Amber (DynamoDB)
        "#3B82F6".toColorInt(), // Blue (PostgreSQL)
        "#8B5CF6".toColorInt(), // Purple (Cassandra)
        "#10B981".toColorInt(), // Emerald (MongoDB)
        "#2DD4BF".toColorInt(), // Cyan (Elasticsearch)
        "#14B8A6".toColorInt(), // Teal
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

        itemCount = 0

        if (data.size > 1) {
            for ((_, dataset) in data.withIndex()) {
                val entry = dataset.entries.firstOrNull() ?: continue
                val label = entry.label ?: dataset.label
                val color = entry.color ?: dataset.color
                if (itemCount < itemPool.size) {
                    val item = itemPool[itemCount]
                    item.label = label; item.value = entry.y; item.color = color
                } else {
                    itemPool.add(HorizontalItem(label, entry.y, color))
                }
                itemCount++
            }
        } else if (data.isNotEmpty()) {
            val dataset = data.first()
            for ((idx, entry) in dataset.entries.withIndex()) {
                val label = entry.label ?: dataset.label
                val color = entry.color ?: dataset.color.takeIf { it != 0 }
                ?: defaultColors[idx % defaultColors.size]
                if (itemCount < itemPool.size) {
                    val item = itemPool[itemCount]
                    item.label = label; item.value = entry.y; item.color = color
                } else {
                    itemPool.add(HorizontalItem(label, entry.y, color))
                }
                itemCount++
            }
        }

        if (itemCount == 0) return

        val leftMargin = 90f * density
        val rightMargin = 48f * density
        val topPadding = config.paddingDp * density + 8f * density
        val bottomPadding = config.paddingDp * density
        val drawWidth = width - leftMargin - rightMargin
        val drawHeight = height - topPadding - bottomPadding
        if (drawWidth <= 0f || drawHeight <= 0f) return

        val slotHeight = drawHeight / itemCount
        val barHeight = (slotHeight * 0.45f).coerceIn(6f * density, 20f * density)
        var maxVal = 1f
        for (i in 0 until itemCount) {
            if (itemPool[i].value > maxVal) maxVal = itemPool[i].value
        }

        for (i in 0 until itemCount) {
            val item = itemPool[i]
            val centerY = topPadding + (i * slotHeight) + (slotHeight / 2f)
            val barTop = centerY - (barHeight / 2f)
            val barBottom = centerY + (barHeight / 2f)

            canvas.drawText(item.label, 8f * density, centerY + 4f * density, labelPaint)

            cachedTrackRect.set(leftMargin, barTop, leftMargin + drawWidth, barBottom)
            canvas.drawRoundRect(cachedTrackRect, barHeight / 2f, barHeight / 2f, trackPaint)

            val fillWidth = (item.value / maxVal) * drawWidth * progress
            if (fillWidth > 0f) {
                fillPaint.color = item.color
                cachedFillRect.set(leftMargin, barTop, leftMargin + fillWidth, barBottom)
                canvas.drawRoundRect(cachedFillRect, barHeight / 2f, barHeight / 2f, fillPaint)
            }

            val valueText = String.format(Locale.US, "%.0fms", item.value)
            canvas.drawText(valueText, width - 8f * density, centerY + 4f * density, valuePaint)
        }
    }
}
