package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Represents a dataset series containing [Entry] points.
 * Configures color, line thickness, point radius, and fill gradients.
 */
public data class Dataset(
    val entries: List<Entry>,
    val label: String = "",
    val color: Int = "#14B8A6".toColorInt(),
    val lineWidth: Float = 1.5f,
    val pointRadius: Float = 2.5f,
    val showPoints: Boolean = true,
    val fillAlpha: Float = 0.15f,
    val visible: Boolean = true,
    val isCurved: Boolean = true,
    val gradientFill: Boolean = false,
    val gradientStartColor: Int? = null,
    val gradientEndColor: Int? = null,
    val useSecondaryAxis: Boolean = false,
    val colors: List<Int>? = null,
    val labelTextColor: Int? = null,
)
