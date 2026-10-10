package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

public class DatasetEdgeCasesTest {

    private val engine = ChartEngine(1000f, 500f, 48f)

    @Test
    public fun `computeRange with single entry dataset`() {
        val dataset = Dataset(entries = listOf(Entry(5f, 42f)))
        val range = engine.computeRange(listOf(dataset))

        assertEquals(5f, range.minX, 0.001f)
        assertEquals(5f, range.maxX, 0.001f)
        assertEquals(42f, range.minY, 0.001f)
        assertEquals(42f, range.maxY, 0.001f)
        assertEquals(0f, range.rangeX, 0.001f)
        assertEquals(0f, range.rangeY, 0.001f)
    }

    @Test
    public fun `toScreenX handles zero rangeX safely without division by zero`() {
        val range = DataRange(minX = 10f, maxX = 10f, minY = 0f, maxY = 100f)
        val screenX = engine.toScreenX(10f, range)

        assertTrue("Screen X must be a valid finite number", screenX.isFinite())
        assertEquals(500f, screenX, 0.001f)
    }

    @Test
    public fun `toScreenY handles zero rangeY safely without division by zero`() {
        val range = DataRange(minX = 0f, maxX = 100f, minY = 50f, maxY = 50f)
        val screenY = engine.toScreenY(50f, range)

        assertTrue("Screen Y must be a valid finite number", screenY.isFinite())
        assertEquals(250f, screenY, 0.001f)
    }

    @Test
    public fun `computeNiceTicks when min equals max returns a valid range step`() {
        val result = AxisTickCalculator.computeNiceTicks(min = 10f, max = 10f)

        assertTrue("niceMax must be greater than niceMin", result.niceMax > result.niceMin)
        assertTrue("Step must be positive", result.step > 0f)
        assertTrue("Ticks list must not be empty", result.ticks.isNotEmpty())
    }

    @Test
    public fun `computeNiceTicks handles negative ranges cleanly`() {
        val result = AxisTickCalculator.computeNiceTicks(min = -50f, max = -10f)

        assertEquals(-50f, result.niceMin, 0.001f)
        assertTrue("niceMax must be at least -10f", result.niceMax >= -10f)
        assertTrue("Ticks list must cover range", result.ticks.first() <= -50f)
    }
}
