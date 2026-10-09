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

    /**
     * Converts ARGB color to HSL (Hue, Saturation, Lightness).
     */
    private fun colorToHsl(color: Int, outHsl: FloatArray) {
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val max = maxOf(r, maxOf(g, b))
        val min = minOf(r, minOf(g, b))
        val delta = max - min

        val l = (max + min) / 2f
        var h = 0f
        var s = 0f

        if (delta != 0f) {
            s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
            h = when (max) {
                r -> (g - b) / delta + (if (g < b) 6f else 0f)
                g -> (b - r) / delta + 2f
                else -> (r - g) / delta + 4f
            }
            h *= 60f
        }

        outHsl[0] = h
        outHsl[1] = s
        outHsl[2] = l
    }

    /**
     * Converts HSL to ARGB Color.
     */
    private fun hslToColor(hsl: FloatArray): Int {
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val ri = ((r + m) * 255f).toInt().coerceIn(0, 255)
        val gi = ((g + m) * 255f).toInt().coerceIn(0, 255)
        val bi = ((b + m) * 255f).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    /**
     * Calculates WCAG relative luminance of a color.
     */
    public fun calculateRelativeLuminance(color: Int): Float {
        fun channelLuminance(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }

        val r = channelLuminance((color ushr 16) and 0xFF)
        val g = channelLuminance((color ushr 8) and 0xFF)
        val b = channelLuminance(color and 0xFF)
        return (0.2126 * r + 0.7152 * g + 0.0722 * b).toFloat()
    }

    /**
     * Calculates a high-contrast text color that stays in the SAME color family (same hue)
     * as [backgroundColor], following WCAG contrast guidelines (>= 4.5:1 ratio).
     */
    public fun calculateHarmoniousContrastColor(backgroundColor: Int): Int {
        val opaqueBg = backgroundColor or 0xFF000000.toInt()
        val hsl = FloatArray(3)
        colorToHsl(opaqueBg, hsl)

        val bgLuminance = calculateRelativeLuminance(opaqueBg)
        val isLightBg = bgLuminance > 0.179f

        val targetH = hsl[0] // Keep exact same hue
        val targetS =
            if (isLightBg) (hsl[1] * 1.1f).coerceAtMost(0.95f) else (hsl[1] * 0.4f).coerceAtMost(
                0.4f
            )
        var targetL = if (isLightBg) 0.10f else 0.95f

        var resultColor = hslToColor(floatArrayOf(targetH, targetS, targetL))

        fun contrastRatio(c1: Int, c2: Int): Float {
            val l1 = calculateRelativeLuminance(c1)
            val l2 = calculateRelativeLuminance(c2)
            return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
        }

        var currentContrast = contrastRatio(resultColor, opaqueBg)
        var attempts = 0
        while (currentContrast < 4.5f && attempts < 15) {
            attempts++
            if (isLightBg) {
                targetL = (targetL - 0.01f).coerceAtLeast(0.01f)
            } else {
                targetL = (targetL + 0.01f).coerceAtMost(0.99f)
            }
            resultColor = hslToColor(floatArrayOf(targetH, targetS, targetL))
            currentContrast = contrastRatio(resultColor, opaqueBg)
        }

        return resultColor
    }
}
