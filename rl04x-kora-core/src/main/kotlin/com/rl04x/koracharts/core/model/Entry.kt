package com.rl04x.koracharts.core.model

/**
 * Represents a data point in the (x, y) coordinate space.
 * All charts work with lists of [Entry].
 *
 * @param x X axis coordinate (index, timestamp, etc.)
 * @param y Y axis value
 * @param label Optional text label for tooltips or axis ticks
 */
public data class Entry(
    val x: Float,
    val y: Float,
    val label: String? = null,
)
