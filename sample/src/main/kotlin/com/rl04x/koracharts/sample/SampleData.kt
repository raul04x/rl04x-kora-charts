package com.rl04x.koracharts.sample

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.model.RadarDataSet
import com.rl04x.koracharts.core.model.StackedBarEntry
import kotlin.math.pow

object SampleData {

    val tealColor = "#14B8A6".toColorInt()
    val purpleColor = "#8B5CF6".toColorInt()
    val yellowColor = "#F59E0B".toColorInt()
    val coralColor = "#F87171".toColorInt()
    val cyanColor = "#2DD4BF".toColorInt()
    val lightPurpleColor = "#A78BFA".toColorInt()
    val emeraldColor = "#10B981".toColorInt()
    val blueColor = "#3B82F6".toColorInt()

    // 1. Line Chart: Expenses Comparison (Full 30 Days of Month)
    fun generateMonthlyExpensesLineData(): Pair<List<Dataset>, Entry> {
        val currentMonthValues = listOf(
            120f, 150f, 190f, 240f, 300f, 350f, 410f, 480f, 530f, 590f,
            640f, 700f, 760f, 830f, 890f, 960f, 1020f, 1080f, 1150f, 1220f,
            1290f, 1360f, 1420f, 1490f, 1560f, 1630f, 1700f, 1760f, 1810f, 1880f
        )
        val previousMonthValues = listOf(
            140f, 180f, 230f, 290f, 360f, 420f, 490f, 560f, 620f, 690f,
            750f, 820f, 890f, 960f, 1030f, 1100f, 1180f, 1250f, 1320f, 1400f,
            1470f, 1550f, 1620f, 1700f, 1780f, 1850f, 1920f, 1990f, 2050f, 2120f
        )

        val currentEntries = (1..30).map { day ->
            val valCurr = currentMonthValues[day - 1]
            Entry(
                x = day.toFloat(),
                y = valCurr,
                label = "Day $day\nExpenses: €${valCurr.toInt()}\nStatus: On Track"
            )
        }
        val previousEntries = (1..30).map { day ->
            val valPrev = previousMonthValues[day - 1]
            Entry(
                x = day.toFloat(),
                y = valPrev,
                label = "Day $day\nPrevious: €${valPrev.toInt()}"
            )
        }

        val currentDataset = Dataset(
            entries = currentEntries,
            label = "Current Month",
            color = tealColor,
            lineWidth = 2f,
            pointRadius = 2.5f,
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
            pointRadius = 2f,
            fillAlpha = 0.08f,
            isCurved = true,
            gradientFill = false,
        )

        return Pair(listOf(currentDataset, previousDataset), currentEntries[19]) // Day 20 selected
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

    // 5. Stacked Bar Data: French Amortization Method Credit (60 Months - Fixed Payment, Decreasing Interest, Increasing Principal)
    fun generateFrenchAmortization60MonthsData(): List<StackedBarEntry> {
        val loanAmount = 10000f
        val monthlyRate = 0.01f // 12% annual rate / 12 = 1% monthly
        val monthsCount = 60

        // Fixed monthly payment formula: A = V * (i * (1+i)^n) / ((1+i)^n - 1)
        val rateFactor = (1.01).pow(60.0).toFloat()
        val monthlyPayment = loanAmount * (monthlyRate * rateFactor) / (rateFactor - 1f)

        var remainingBalance = loanAmount
        val colors = listOf(
            emeraldColor,
            coralColor
        ) // Principal Repayment (Emerald Green), Interest Payment (Coral Red)
        val segmentLabels = listOf("Principal Repayment", "Interest Payment")

        val entries = mutableListOf<StackedBarEntry>()

        for (month in 1..monthsCount) {
            val interestPayment = remainingBalance * monthlyRate
            val principalPayment = (monthlyPayment - interestPayment).coerceAtLeast(0f)
            remainingBalance = (remainingBalance - principalPayment).coerceAtLeast(0f)

            entries.add(
                StackedBarEntry(
                    x = month.toFloat(),
                    values = listOf(principalPayment, interestPayment),
                    colors = colors,
                    label = "Month $month",
                    textColors = null,
                    segmentLabels = segmentLabels,
                )
            )
        }

        return entries
    }

    fun generateQuarterlyRevenueStackedData(): List<StackedBarEntry> =
        generateFrenchAmortization60MonthsData()

    fun generateSalesChannelStackedData(): List<StackedBarEntry> =
        generateFrenchAmortization60MonthsData()

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
