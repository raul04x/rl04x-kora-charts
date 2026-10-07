package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

public class ChartEngineTest {

    private val engine: ChartEngine = ChartEngine(
        viewWidth = 1000f,
        viewHeight = 500f,
        paddingPx = 50f,
    )

    @Test
    public fun `computeRange with empty datasets returns default range`() {
        val range = engine.computeRange(emptyList())
        assertEquals(0f, range.minX, 0.001f)
        assertEquals(1f, range.maxX, 0.001f)
        assertEquals(0f, range.minY, 0.001f)
        assertEquals(1f, range.maxY, 0.001f)
    }

    @Test
    public fun `computeRange computes correct min and max values`() {
        val dataset = Dataset(
            entries = listOf(
                Entry(0f, 10f),
                Entry(5f, 50f),
                Entry(10f, 20f),
            ),
        )
        val range = engine.computeRange(listOf(dataset))

        assertEquals(0f, range.minX, 0.001f)
        assertEquals(10f, range.maxX, 0.001f)
        assertEquals(10f, range.minY, 0.001f)
        assertEquals(50f, range.maxY, 0.001f)
        assertEquals(10f, range.rangeX, 0.001f)
        assertEquals(40f, range.rangeY, 0.001f)
    }

    @Test
    public fun `toScreenX converts minX to padding and maxX to width minus padding`() {
        val range = DataRange(minX = 0f, maxX = 10f, minY = 0f, maxY = 100f)

        // padding = 50, drawWidth = 900
        val minScreenX = engine.toScreenX(0f, range)
        val maxScreenX = engine.toScreenX(10f, range)
        val midScreenX = engine.toScreenX(5f, range)

        assertEquals(50f, minScreenX, 0.001f)
        assertEquals(950f, maxScreenX, 0.001f)
        assertEquals(500f, midScreenX, 0.001f)
    }

    @Test
    public fun `toScreenY inverts coordinates so minY is at bottom and maxY is at top`() {
        val range = DataRange(minX = 0f, maxX = 10f, minY = 0f, maxY = 100f)

        // padding = 50, drawHeight = 400
        val maxScreenY = engine.toScreenY(100f, range)
        val minScreenY = engine.toScreenY(0f, range)

        assertEquals(50f, maxScreenY, 0.001f)
        assertEquals(450f, minScreenY, 0.001f)
    }

    @Test
    public fun `nearestEntry finds entry closest to touch coordinate`() {
        val entry1 = Entry(0f, 10f, "E1")
        val entry2 = Entry(10f, 100f, "E2")
        val dataset = Dataset(entries = listOf(entry1, entry2))
        val range = engine.computeRange(listOf(dataset))

        // Touch near (0, 10) in screen space
        val screenX1 = engine.toScreenX(0f, range)
        val screenY1 = engine.toScreenY(10f, range)

        val nearest = engine.nearestEntry(screenX1 + 5f, screenY1 + 5f, listOf(dataset), range)
        assertNotNull(nearest)
        assertEquals("E1", nearest?.label)
    }
}
