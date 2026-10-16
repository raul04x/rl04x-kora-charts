package com.rl04x.koracharts.core.engine

import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.renderer.helper.LegendRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

public class PieEntrySelectionTest {

    private val entries = listOf(
        Entry(0f, 1f, "First"),
        Entry(1f, 3f, "Second"),
    )
    private val dataset = Dataset(entries = entries)

    @Test
    public fun `findPieEntryAt selects each rendered slice`() {
        val config = ChartConfig(paddingDp = 16f)
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)
        val layout = layoutFor(entries, config)

        assertEquals(
            entries[0],
            engine.findPieEntryAt(
                pointX(layout, -45f),
                pointY(layout, -45f),
                listOf(dataset),
                config = config,
            ),
        )
        assertEquals(
            entries[1],
            engine.findPieEntryAt(
                pointX(layout, 225f),
                pointY(layout, 225f),
                listOf(dataset),
                config = config,
            ),
        )
    }

    @Test
    public fun `findPieEntryAt uses legend and label layout from renderer`() {
        val config = ChartConfig(
            paddingDp = 16f,
            showLegend = true,
            showAxisLabels = true,
        )
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)
        val layout = layoutFor(entries, config)

        assertEquals(
            entries[0],
            engine.findPieEntryAt(
                pointX(layout, -45f),
                pointY(layout, -45f),
                listOf(dataset),
                config = config,
            ),
        )
    }

    @Test
    public fun `findPieEntryAt rejects donut hole and points outside the pie`() {
        val config = ChartConfig(paddingDp = 16f)
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)
        val layout = layoutFor(entries, config)

        assertNull(
            engine.findPieEntryAt(
                layout.centerX,
                layout.centerY,
                listOf(dataset),
                config = config,
            ),
        )
        assertNull(
            engine.findPieEntryAt(
                layout.centerX + layout.radius + 1f,
                layout.centerY,
                listOf(dataset),
                config = config,
            ),
        )
    }

    @Test
    public fun `findPieEntryAt allows selecting the center of a solid pie`() {
        val config = ChartConfig(paddingDp = 16f)
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)
        val layout = layoutFor(entries, config)

        assertEquals(
            entries[0],
            engine.findPieEntryAt(
                screenX = layout.centerX,
                screenY = layout.centerY,
                datasets = listOf(dataset),
                config = config,
                holeRadiusRatio = 0f,
            ),
        )
    }

    @Test
    public fun `findPieEntryAt returns null for empty or non-positive datasets`() {
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)

        assertNull(engine.findPieEntryAt(200f, 100f, emptyList()))
        assertNull(
            engine.findPieEntryAt(
                200f,
                100f,
                listOf(Dataset(entries = listOf(Entry(0f, 0f, "Zero")))),
            ),
        )
    }

    @Test
    public fun `findPieEntryAt accounts for selected slice explosion`() {
        val config = ChartConfig(paddingDp = 16f, selectedEntry = entries[0])
        val engine = ChartEngine(viewWidth = 400f, viewHeight = 300f, paddingPx = 16f)
        val layout = layoutFor(entries, config)
        val angle = -45f
        val explodedCenterX = layout.centerX + 5f * cos(Math.toRadians(angle.toDouble())).toFloat()
        val explodedCenterY = layout.centerY + 5f * sin(Math.toRadians(angle.toDouble())).toFloat()

        assertEquals(
            entries[0],
            engine.findPieEntryAt(
                explodedCenterX + layout.radius * 0.75f * cos(Math.toRadians(angle.toDouble())).toFloat(),
                explodedCenterY + layout.radius * 0.75f * sin(Math.toRadians(angle.toDouble())).toFloat(),
                listOf(dataset),
                config = config,
            ),
        )
    }

    private fun layoutFor(entries: List<Entry>, config: ChartConfig): PieChartLayout {
        val density = 1f
        val padding = config.paddingDp * density
        val legendHeight = if (config.showLegend && entries.isNotEmpty()) {
            LegendRenderer().calculateLegendHeight(
                labels = entries.mapIndexed { index, entry ->
                    entry.label?.substringBefore('\n') ?: "Category ${index + 1}"
                },
                maxLegendWidth = 400f - padding * 2f,
                density = density,
            )
        } else {
            0f
        }
        return PieChartLayout.calculate(
            width = 400f,
            height = 300f,
            paddingPx = padding,
            density = density,
            entryCount = entries.size,
            config = config,
            legendHeight = legendHeight,
        )
    }

    private fun pointX(layout: PieChartLayout, angleDegrees: Float): Float {
        val angle = Math.toRadians(angleDegrees.toDouble())
        return layout.centerX + layout.radius * 0.75f * cos(angle).toFloat()
    }

    private fun pointY(layout: PieChartLayout, angleDegrees: Float): Float {
        val angle = Math.toRadians(angleDegrees.toDouble())
        return layout.centerY + layout.radius * 0.75f * sin(angle).toFloat()
    }
}
