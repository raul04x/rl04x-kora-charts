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
        assertEquals("€1.5K/mo", NumberFormatterUtils.formatCompact(1500f, valuePrefix = "€", valueSuffix = "/mo"))
    }
}
