package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Represents a bubble scatter point (x, y, radius, color).
 */
public data class BubbleEntry(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val color: Int = "#14B8A6".toColorInt(),
    val label: String? = null,
)
