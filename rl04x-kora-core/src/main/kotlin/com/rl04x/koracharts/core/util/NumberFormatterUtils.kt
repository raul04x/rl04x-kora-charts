package com.rl04x.koracharts.core.util

import java.util.Locale
import kotlin.math.abs

/**
 * Utility functions for compact number formatting (e.g. 1.2K, 3.5M, 2B).
 */
public object NumberFormatterUtils {

    /**
     * Formats a numeric value compactly:
     * - Values >= 1,000,000,000 -> e.g. "1.2B"
     * - Values >= 1,000,000 -> e.g. "3.5M"
     * - Values >= 1,000 -> e.g. "2.5K"
     * - Values < 1,000 -> e.g. "850" or "12.5"
     */
    public fun formatCompact(
        value: Float,
        valuePrefix: String = "",
        valueSuffix: String = "",
    ): String {
        val absVal = abs(value)
        val formattedNumber = when {
            absVal >= 1_000_000_000f -> {
                val v = value / 1_000_000_000f
                if (v == v.toLong().toFloat()) String.format(
                    Locale.US,
                    "%.0fB",
                    v
                ) else String.format(Locale.US, "%.1fB", v)
            }

            absVal >= 1_000_000f -> {
                val v = value / 1_000_000f
                if (v == v.toLong().toFloat()) String.format(
                    Locale.US,
                    "%.0fM",
                    v
                ) else String.format(Locale.US, "%.1fM", v)
            }

            absVal >= 1_000f -> {
                val v = value / 1_000f
                if (v == v.toLong().toFloat()) String.format(
                    Locale.US,
                    "%.0fK",
                    v
                ) else String.format(Locale.US, "%.1fK", v)
            }

            else -> {
                if (value == value.toLong().toFloat()) String.format(
                    Locale.US,
                    "%.0f",
                    value
                ) else String.format(Locale.US, "%.1f", value)
            }
        }
        return "$valuePrefix$formattedNumber$valueSuffix"
    }
}
