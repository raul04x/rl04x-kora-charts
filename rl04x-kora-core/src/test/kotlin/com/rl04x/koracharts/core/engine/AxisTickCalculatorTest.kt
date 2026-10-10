package com.rl04x.koracharts.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class AxisTickCalculatorTest {

    @Test
    public fun `computeNiceStep generates 5 for range 0 to 22`() {
        val step = AxisTickCalculator.computeNiceStep(range = 22f, targetTicks = 4)
        assertEquals(5f, step, 0.001f)
    }

    @Test
    public fun `computeNiceStep generates 100 for range 0 to 450`() {
        val step = AxisTickCalculator.computeNiceStep(range = 450f, targetTicks = 4)
        assertEquals(100f, step, 0.001f)
    }

    @Test
    public fun `computeNiceStep generates 1000 for range 0 to 3500`() {
        val step = AxisTickCalculator.computeNiceStep(range = 3500f, targetTicks = 4)
        assertEquals(1000f, step, 0.001f)
    }

    @Test
    public fun `computeNiceTicks returns ticks capped at max for range 0 to 22`() {
        val result = AxisTickCalculator.computeNiceTicks(min = 0f, max = 22f, targetTicks = 4)
        assertEquals(5f, result.step, 0.001f)
        assertEquals(listOf(0f, 5f, 10f, 15f, 22f), result.ticks)
        assertEquals(0f, result.niceMin, 0.001f)
        assertEquals(22f, result.niceMax, 0.001f)
    }

    @Test
    public fun `computeNiceTicks returns ticks capped at max for range 0 to 450`() {
        val result = AxisTickCalculator.computeNiceTicks(min = 0f, max = 450f, targetTicks = 4)
        assertEquals(100f, result.step, 0.001f)
        assertEquals(listOf(0f, 100f, 200f, 300f, 400f, 450f), result.ticks)
    }

    @Test
    public fun `computeNiceTicks respects customStep parameter`() {
        val result = AxisTickCalculator.computeNiceTicks(min = 0f, max = 50f, customStep = 10f)
        assertEquals(10f, result.step, 0.001f)
        assertEquals(listOf(0f, 10f, 20f, 30f, 40f, 50f), result.ticks)
    }

    @Test
    public fun `computeNiceTicks with forceInteger ensures step is at least 1`() {
        val result =
            AxisTickCalculator.computeNiceTicks(min = 0.1f, max = 0.8f, forceInteger = true)
        assertTrue("Step must be at least 1.0", result.step >= 1f)
        assertEquals(listOf(0f, 1f), result.ticks)
    }

    @Test
    public fun `computeNiceTicks with capAtMax caps upper tick at max value`() {
        val result = AxisTickCalculator.computeNiceTicks(
            min = 0f,
            max = 22f,
            targetTicks = 4,
            capAtMax = true
        )
        assertEquals(22f, result.niceMax, 0.001f)
        assertEquals(listOf(0f, 5f, 10f, 15f, 22f), result.ticks)
    }

    @Test
    public fun `computeNiceTicks with capAtMax for range 0 to 450 caps at 450`() {
        val result = AxisTickCalculator.computeNiceTicks(
            min = 0f,
            max = 450f,
            targetTicks = 4,
            capAtMax = true
        )
        assertEquals(450f, result.niceMax, 0.001f)
        assertEquals(listOf(0f, 100f, 200f, 300f, 400f, 450f), result.ticks)
    }
}
