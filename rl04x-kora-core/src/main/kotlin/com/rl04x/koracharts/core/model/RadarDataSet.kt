package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Dataset model for Radar/Spider web charts.
 */
public data class RadarDataSet(
    val label: String,
    val values: List<Float>,
    val color: Int = "#14B8A6".toColorInt(),
    val fillAlpha: Float = 0.25f,
)
