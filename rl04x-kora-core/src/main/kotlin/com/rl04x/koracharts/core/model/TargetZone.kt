package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Represents a translucent horizontal target range or alert zone.
 */
public data class TargetZone(
    val minY: Float,
    val maxY: Float,
    val label: String? = null,
    val color: Int = "#10B981".toColorInt(),
    val fillAlpha: Float = 0.12f,
)
