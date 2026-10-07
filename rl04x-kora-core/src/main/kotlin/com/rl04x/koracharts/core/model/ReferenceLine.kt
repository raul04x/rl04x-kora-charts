package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Represents a horizontal reference or threshold line drawn across the chart canvas.
 */
public data class ReferenceLine(
    val value: Float,
    val label: String? = null,
    val color: Int = "#EF4444".toColorInt(),
    val isDashed: Boolean = true,
    val strokeWidthDp: Float = 1.5f,
)
