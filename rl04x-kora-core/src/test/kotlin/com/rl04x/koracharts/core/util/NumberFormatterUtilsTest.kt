package com.rl04x.koracharts.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

public class NumberFormatterUtilsTest {

    @Test
    public fun `formatCompact formats thousands correctly`() {
        assertEquals("1.5K", NumberFormatterUtils.formatCompact(1500f))
        assertEquals("2K", NumberFormatterUtils.formatCompact(2000f))
    }

    @Test
    public fun `formatCompact formats millions correctly`() {
        assertEquals("2.4M", NumberFormatterUtils.formatCompact(2400000f))
        assertEquals("5M", NumberFormatterUtils.formatCompact(5000000f))
    }

    @Test
    public fun `formatCompact formats billions correctly`() {
        assertEquals("1B", NumberFormatterUtils.formatCompact(1000000000f))
        assertEquals("3.2B", NumberFormatterUtils.formatCompact(3200000000f))
    }

    @Test
    public fun `formatCompact includes prefix and suffix`() {
        assertEquals(
            "€1.5K/mo",
            NumberFormatterUtils.formatCompact(1500f, valuePrefix = "€", valueSuffix = "/mo")
        )
    }

    @Test
    public fun `calculateHarmoniousContrastColor produces WCAG contrast color in same hue family`() {
        fun ratio(c1: Int, c2: Int): Float {
            val l1 = NumberFormatterUtils.calculateRelativeLuminance(c1)
            val l2 = NumberFormatterUtils.calculateRelativeLuminance(c2)
            return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
        }

        val brightYellow = 0xFFF59E0B.toInt()
        val contrastYellowText = NumberFormatterUtils.calculateHarmoniousContrastColor(brightYellow)
        val contrastYellowRatio = ratio(contrastYellowText, brightYellow)
        org.junit.Assert.assertTrue(
            "Contrast ratio must meet WCAG AA >= 4.5",
            contrastYellowRatio >= 4.5f
        )

        val darkIndigo = 0xFF3730A3.toInt()
        val contrastIndigoText = NumberFormatterUtils.calculateHarmoniousContrastColor(darkIndigo)
        val contrastIndigoRatio = ratio(contrastIndigoText, darkIndigo)
        org.junit.Assert.assertTrue(
            "Contrast ratio must meet WCAG AA >= 4.5",
            contrastIndigoRatio >= 4.5f
        )
    }
}
