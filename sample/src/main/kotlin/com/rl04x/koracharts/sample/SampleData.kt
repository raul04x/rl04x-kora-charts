package com.rl04x.koracharts.sample

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.model.RadarDataSet
import com.rl04x.koracharts.core.model.StackedBarEntry

object SampleData {

    val tealColor = "#14B8A6".toColorInt()
    val purpleColor = "#8B5CF6".toColorInt()
    val yellowColor = "#F59E0B".toColorInt()
    val coralColor = "#F87171".toColorInt()
    val cyanColor = "#2DD4BF".toColorInt()
    val lightPurpleColor = "#A78BFA".toColorInt()
    val emeraldColor = "#10B981".toColorInt()
    val blueColor = "#3B82F6".toColorInt()

    // 1. Line Chart: Expenses Comparison
    fun generateMonthlyExpensesLineData(): Pair<List<Dataset>, Entry> {
        val days = listOf("Day 1", "Day 5", "Day 10", "Day 15", "Day 20", "Day 25", "Day 30")
        val currentMonthValues = listOf(120f, 340f, 580f, 820f, 1146f, 1420f, 1750f)
        val previousMonthValues = listOf(150f, 410f, 690f, 980f, 1310f, 1650f, 1920f)

        val currentEntries = days.mapIndexed { idx, d ->
            Entry(
                idx.toFloat() * 5f + 1f,
                currentMonthValues[idx],
                "$d\nExpenses: €${currentMonthValues[idx].toInt()}\nStatus: On Track"
            )
        }
        val previousEntries = days.mapIndexed { idx, d ->
            Entry(
                idx.toFloat() * 5f + 1f,
                previousMonthValues[idx],
                "$d\nPrevious: €${previousMonthValues[idx].toInt()}"
            )
        }

        val currentDataset = Dataset(
            entries = currentEntries,
            label = "Current Month",
            color = tealColor,
            lineWidth = 2f,
            pointRadius = 3f,
            fillAlpha = 0.22f,
            isCurved = true,
            gradientFill = true,
            gradientStartColor = tealColor,
            gradientEndColor = Color.TRANSPARENT,
        )

        val previousDataset = Dataset(
            entries = previousEntries,
            label = "Previous Month",
            color = "#818CF8".toColorInt(),
            lineWidth = 1.8f,
            pointRadius = 2.5f,
            fillAlpha = 0.08f,
            isCurved = true,
            gradientFill = false,
        )

        return Pair(listOf(currentDataset, previousDataset), currentEntries[4])
    }

    // 2. Bar Chart: Product Sales
    fun generateCategorySalesBarData(): Pair<List<Dataset>, Entry> {
        val categories =
            listOf("Electronics", "Clothing", "Home", "Sports", "Books", "Toys", "Beauty", "Auto")
        val values = listOf(42f, 65f, 52f, 88f, 70f, 95f, 60f, 78f)

        val entries = categories.mapIndexed { idx, cat ->
            Entry(idx.toFloat(), values[idx], "$cat\nSales: €${values[idx].toInt()}k\nTarget: €80k")
        }
        val selectedEntry = entries[5]

        val dataset = Dataset(
            entries = entries,
            label = "Sales",
            color = cyanColor,
            lineWidth = 2f,
        )

        return Pair(listOf(dataset), selectedEntry)
    }

    // 3. Horizontal Bar: Latency (Distinct Vibrant Colors per Engine)
    fun generateDatabaseLatencyHorizontalData(): List<Dataset> {
        val items = listOf(
            Triple("Redis", 12f, coralColor),
            Triple("DynamoDB", 24f, yellowColor),
            Triple("PostgreSQL", 28f, blueColor),
            Triple("Cassandra", 35f, purpleColor),
            Triple("MongoDB", 48f, emeraldColor),
            Triple("Elasticsearch", 62f, cyanColor),
        )
        return items.map { (name, latency, itemColor) ->
            Dataset(
                entries = listOf(Entry(0f, latency, name, color = itemColor)),
                label = name,
                color = itemColor,
            )
        }
    }

    // 4. Pie Data: Rich Multi-Sector Device Distribution
    fun generateMarketSharePieData(): List<Dataset> {
        val entries = listOf(
            Entry(0f, 32f, "Mobile"),
            Entry(1f, 22f, "Desktop"),
            Entry(2f, 14f, "Tablet"),
            Entry(3f, 10f, "Smart TV"),
            Entry(4f, 8f, "Wearables"),
            Entry(5f, 6f, "Automotive"),
            Entry(6f, 5f, "Consoles"),
            Entry(7f, 3f, "IoT Devices"),
        )
        return listOf(Dataset(entries = entries))
    }

    fun generateBrowserSharePieData(): List<Dataset> = generateMarketSharePieData()

    // 5. Stacked Bar Data: 8 Months with 5 Regional Segments
    fun generateQuarterlyRevenueStackedData(): List<StackedBarEntry> {
        val colors = listOf(tealColor, purpleColor, yellowColor, coralColor, blueColor)
        val segmentLabels = listOf(
            "North America",
            "Europe",
            "Asia Pacific",
            "Latin America",
            "Middle East & Africa"
        )

        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")
        val matrixValues = listOf(
            listOf(25f, 30f, 15f, 10f, 8f),
            listOf(28f, 32f, 18f, 12f, 10f),
            listOf(32f, 38f, 22f, 15f, 11f),
            listOf(30f, 35f, 20f, 14f, 9f),
            listOf(36f, 42f, 25f, 18f, 12f),
            listOf(40f, 45f, 28f, 20f, 14f),
            listOf(38f, 40f, 26f, 17f, 13f),
            listOf(44f, 48f, 32f, 22f, 16f),
        )

        return months.mapIndexed { idx, monthName ->
            StackedBarEntry(
                x = idx.toFloat(),
                values = matrixValues[idx],
                colors = colors,
                label = monthName,
                textColors = null,
                segmentLabels = segmentLabels,
            )
        }
    }

    fun generateSalesChannelStackedData(): List<StackedBarEntry> =
        generateQuarterlyRevenueStackedData()

    // 6. Candlestick Data
    fun generateFinancialCandlestickData(): List<CandlestickEntry> {
        return listOf(
            CandlestickEntry(0f, 100f, 115f, 95f, 110f, "Mon"),
            CandlestickEntry(1f, 110f, 125f, 105f, 108f, "Tue"),
            CandlestickEntry(2f, 108f, 130f, 102f, 122f, "Wed"),
            CandlestickEntry(3f, 122f, 128f, 112f, 115f, "Thu"),
            CandlestickEntry(4f, 115f, 140f, 110f, 135f, "Fri"),
        )
    }

    fun generateCryptoCandlestickData(): List<CandlestickEntry> = generateFinancialCandlestickData()

    // 7. Radar Data
    fun generateSkillMatrixRadarData(): List<RadarDataSet> {
        return listOf(
            RadarDataSet("Current Skill", listOf(90f, 85f, 70f, 95f, 60f, 80f), tealColor, 0.25f),
            RadarDataSet("Target Skill", listOf(70f, 90f, 85f, 75f, 80f, 90f), purpleColor, 0.15f),
        )
    }

    fun generateDeveloperRadarData(): List<RadarDataSet> = generateSkillMatrixRadarData()

    // 8. Combined Data
    fun generateCombinedData(): List<Dataset> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activeUsers = listOf(45f, 62f, 58f, 80f, 92f, 75f, 68f)
        val conversions = listOf(32f, 48f, 42f, 65f, 78f, 58f, 52f)

        val barEntries = days.mapIndexed { idx, d -> Entry(idx.toFloat(), activeUsers[idx], d) }
        val lineEntries = days.mapIndexed { idx, d -> Entry(idx.toFloat(), conversions[idx], d) }

        return listOf(
            Dataset(entries = barEntries, label = "Users", color = lightPurpleColor),
            Dataset(
                entries = lineEntries,
                label = "Conversions",
                color = yellowColor,
                lineWidth = 3f
            ),
        )
    }

    fun generateTrafficCombinedData(): List<Dataset> = generateCombinedData()

    // 9. Bubble Data
    fun generateProjectRiskBubbleData(): List<BubbleEntry> {
        val projects = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7")
        val risks = listOf(15f, 28f, 42f, 58f, 68f, 78f, 88f)
        val returns = listOf(35f, 62f, 48f, 85f, 55f, 92f, 70f)
        val budgets = listOf(16f, 24f, 20f, 30f, 22f, 32f, 26f)
        val colors = listOf(
            emeraldColor,
            blueColor,
            cyanColor,
            yellowColor,
            lightPurpleColor,
            coralColor,
            purpleColor
        )

        return projects.mapIndexed { idx, p ->
            BubbleEntry(
                x = risks[idx],
                y = returns[idx],
                radiusDp = budgets[idx],
                color = colors[idx],
                label = p,
            )
        }
    }

    // 10. Revenue Thousands Line Data
    fun generateRevenueThousandsLineData(): Pair<List<Dataset>, Entry> {
        val months = listOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
        )
        val values =
            listOf(250f, 480f, 750f, 1100f, 1450f, 1800f, 2150f, 2400f, 2850f, 3100f, 3450f, 3800f)

        val entries = months.mapIndexed { idx, m ->
            Entry(idx.toFloat(), values[idx], "$m: €${values[idx].toInt()}k")
        }

        val dataset = Dataset(
            entries = entries,
            label = "Revenue (€k)",
            color = emeraldColor,
            lineWidth = 2f,
            pointRadius = 3f,
            fillAlpha = 0.25f,
            isCurved = true,
            gradientFill = true,
            gradientStartColor = emeraldColor,
            gradientEndColor = Color.TRANSPARENT,
        )

        return Pair(listOf(dataset), entries.last())
    }
}
