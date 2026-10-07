package com.rl04x.koracharts.core.model

/**
 * Represents a stacked bar entry containing multiple data segment values and colors.
 */
public data class StackedBarEntry(
    val x: Float,
    val values: List<Float>,
    val colors: List<Int>,
    val label: String? = null,
    val textColors: List<Int>? = null,
)
