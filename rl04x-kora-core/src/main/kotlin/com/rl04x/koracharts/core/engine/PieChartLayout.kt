package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.ChartConfig

internal data class PieChartLayout(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
) {
    companion object {
        fun calculate(
            width: Float,
            height: Float,
            paddingPx: Float,
            density: Float,
            entryCount: Int,
            config: ChartConfig,
            legendHeight: Float,
        ): PieChartLayout {
            val topMargin = if (config.showLegend && entryCount > 0) {
                paddingPx + legendHeight + 6f * density
            } else {
                paddingPx
            }
            val outerMargin = if (config.showAxisLabels && entryCount > 2) {
                28f * density
            } else {
                12f * density
            }
            val availableSize =
                minOf(width - paddingPx * 2f, height - topMargin - paddingPx) - outerMargin * 2f
            val radius = maxOf(availableSize, 40f * density) / 2f
            val centerY =
                topMargin + outerMargin + radius +
                        (height - topMargin - outerMargin * 2f - radius * 2f) / 2f

            return PieChartLayout(
                centerX = width / 2f,
                centerY = centerY,
                radius = radius,
            )
        }
    }
}
