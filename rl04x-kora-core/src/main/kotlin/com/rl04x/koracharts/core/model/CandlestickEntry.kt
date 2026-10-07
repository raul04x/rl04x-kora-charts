package com.rl04x.koracharts.core.model

/**
 * Represents a financial candlestick data point (open, high, low, close).
 */
public data class CandlestickEntry(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val label: String? = null,
)
