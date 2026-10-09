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

    // 1. Line Chart: Expenses Comparison (Current Month vs Previous Month)
    fun generateMonthlyExpensesLineData(): Pair<List<Dataset>, Entry> {
        val days = listOf("Day 1", "Day 5", "Day 10", "Day 15", "Day 20", "Day 25", "Day 30")
        val currentMonthValues = listOf(120f, 340f, 580f, 820f, 1146f, 1420f, 1750f)
        val previousMonthValues = listOf(150f, 410f, 690f, 980f, 1310f, 1650f, 1920f)

        val currentEntries = days.mapIndexed { idx, d ->
            Entry(idx.toFloat() * 5f + 1f, currentMonthValues[idx], d)
        }
        val previousEntries = days.mapIndexed { idx, d ->
            Entry(idx.toFloat() * 5f + 1f, previousMonthValues[idx], d)
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

    // 2. Bar Chart: Product Sales by Category
    fun generateCategorySalesBarData(): Pair<List<Dataset>, Entry> {
        val categories =
            listOf("Electronics", "Clothing", "Home", "Sports", "Books", "Toys", "Beauty", "Auto")
        val values = listOf(42f, 65f, 52f, 88f, 70f, 95f, 60f, 78f)

        val entries = categories.mapIndexed { idx, cat ->
            Entry(idx.toFloat(), values[idx], "$cat (€${values[idx].toInt()}k)")
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

    // 3. Horizontal Bar: Database Engine Latency (ms)
    fun generateDatabaseLatencyHorizontalData(): List<Dataset> {
        val items = listOf(
            Pair("Redis", 12f),
            Pair("DynamoDB", 24f),
            Pair("PostgreSQL", 28f),
            Pair("Cassandra", 35f),
            Pair("MongoDB", 48f),
            Pair("Elasticsearch", 62f),
        )

        val colors =
            listOf(emeraldColor, cyanColor, tealColor, yellowColor, coralColor, purpleColor)

        return items.mapIndexed { idx, pair ->
            Dataset(
                entries = listOf(Entry(idx.toFloat(), pair.second, pair.first)),
                label = pair.first,
                color = colors[idx],
            )
        }
    }

    // 4. Donut/Pie: Web Browser Market Share
    fun generateBrowserSharePieData(): List<Dataset> {
        val entries = listOf(
            Entry(1f, 45f, "Chrome", color = yellowColor),
            Entry(2f, 22f, "Safari", color = blueColor),
            Entry(3f, 12f, "Firefox", color = purpleColor),
            Entry(4f, 7f, "Edge", color = cyanColor),
            Entry(5f, 5f, "Opera", color = coralColor),
            Entry(6f, 4f, "Brave", color = emeraldColor),
            Entry(7f, 3f, "Vivaldi", color = tealColor),
            Entry(8f, 2f, "Others", color = lightPurpleColor),
        )
        return listOf(Dataset(entries = entries))
    }

    // 5. Stacked Bar: Revenue by Sales Channel
    fun generateSalesChannelStackedData(): List<StackedBarEntry> {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")
        val colors = listOf(tealColor, coralColor, yellowColor)

        return months.mapIndexed { idx, m ->
            StackedBarEntry(
                x = idx.toFloat(),
                values = listOf(25f + idx * 3f, 18f + (idx % 4) * 6f, 12f + (idx % 3) * 7f),
                colors = colors,
                label = m,
            )
        }
    }

    // 6. Candlestick: KORA / USD Daily Candles
    fun generateCryptoCandlestickData(): List<CandlestickEntry> {
        val days = listOf("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10")
        val opens = listOf(2.10f, 2.45f, 2.30f, 2.80f, 3.10f, 3.65f, 3.40f, 3.90f, 3.75f, 4.20f)
        val closes = listOf(2.45f, 2.25f, 2.85f, 3.15f, 3.70f, 3.35f, 3.95f, 3.70f, 4.30f, 4.65f)
        val highs = listOf(2.55f, 2.60f, 2.95f, 3.30f, 3.85f, 3.80f, 4.10f, 4.05f, 4.45f, 4.80f)
        val lows = listOf(2.00f, 2.15f, 2.20f, 2.70f, 3.00f, 3.25f, 3.30f, 3.60f, 3.65f, 4.10f)

        return days.mapIndexed { idx, day ->
            CandlestickEntry(
                x = idx.toFloat(),
                open = opens[idx],
                high = highs[idx],
                low = lows[idx],
                close = closes[idx],
                label = day,
            )
        }
    }

    // 7. Radar Data: Developer Skill Assessment
    fun generateDeveloperRadarData(): List<RadarDataSet> {
        return listOf(
            RadarDataSet(
                label = "Senior Dev",
                values = listOf(92f, 88f, 95f, 82f, 85f, 78f),
                color = tealColor,
                fillAlpha = 0.35f,
            ),
            RadarDataSet(
                label = "Junior Dev",
                values = listOf(65f, 60f, 50f, 70f, 55f, 60f),
                color = purpleColor,
                fillAlpha = 0.35f,
            ),
        )
    }

    // 8. Combined Data: Active Users vs Conversions
    fun generateTrafficCombinedData(): List<Dataset> {
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

    // 9. Bubble Data: Project Portfolio Matrix
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

    // 10. Line Chart: Annual Revenue in Thousands (€100k to €3.8M)
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
