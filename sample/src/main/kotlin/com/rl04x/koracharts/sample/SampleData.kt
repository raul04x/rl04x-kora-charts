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

    // Paleta de colores atractiva
    val tealColor = "#14B8A6".toColorInt()
    val purpleColor = "#8B5CF6".toColorInt()
    val yellowColor = "#F59E0B".toColorInt()
    val coralColor = "#F87171".toColorInt()
    val cyanColor = "#2DD4BF".toColorInt()
    val lightPurpleColor = "#A78BFA".toColorInt()
    val emeraldColor = "#10B981".toColorInt()
    val blueColor = "#3B82F6".toColorInt()

    // 1. Line Chart: Comparación de Tráfico de Red (Mbps) 2025 vs 2024
    fun generateNetworkTrafficLineData(): Pair<List<Dataset>, Entry> {
        val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val values2025 = listOf(18f, 29f, 24f, 45f, 62f, 55f, 84f, 78f, 92f, 88f, 105f, 96f)
        val values2024 = listOf(12f, 20f, 18f, 32f, 48f, 42f, 65f, 60f, 75f, 70f, 82f, 80f)

        val entries2025 = months.mapIndexed { idx, m ->
            Entry(idx.toFloat(), values2025[idx], "$m 2025: ${values2025[idx]} Mbps")
        }
        val entries2024 = months.mapIndexed { idx, m ->
            Entry(idx.toFloat(), values2024[idx], "$m 2024: ${values2024[idx]} Mbps")
        }

        val dataset2025 = Dataset(
            entries = entries2025,
            label = "Tráfico 2025",
            color = tealColor,
            lineWidth = 1.5f,
            pointRadius = 2.5f,
            fillAlpha = 0.2f,
            isCurved = true,
            gradientFill = true,
            gradientStartColor = tealColor,
            gradientEndColor = Color.TRANSPARENT,
        )

        val dataset2024 = Dataset(
            entries = entries2024,
            label = "Tráfico 2024",
            color = "#818CF8".toColorInt(),
            lineWidth = 1.5f,
            pointRadius = 2.5f,
            fillAlpha = 0.1f,
            isCurved = true,
            gradientFill = false,
        )

        return Pair(listOf(dataset2025, dataset2024), entries2025[6])
    }

    // 2. Bar Chart: Ventas por Categoría de Producto
    fun generateCategorySalesBarData(): Pair<List<Dataset>, Entry> {
        val categories = listOf("Electro", "Ropa", "Hogar", "Deportes", "Libros", "Juguetes", "Belleza", "Motor")
        val values = listOf(42f, 65f, 52f, 88f, 70f, 95f, 60f, 78f)

        val entries = categories.mapIndexed { idx, cat ->
            Entry(idx.toFloat(), values[idx], "$cat (€${values[idx].toInt()}k)")
        }
        val selectedEntry = entries[5] // Juguetes (€95k)

        val dataset = Dataset(
            entries = entries,
            label = "Ventas",
            color = cyanColor,
            lineWidth = 2f,
        )

        return Pair(listOf(dataset), selectedEntry)
    }

    // 3. Horizontal Bar: Latencia de Motores de Base de Datos (ms)
    fun generateDatabaseLatencyHorizontalData(): List<Dataset> {
        val items = listOf(
            Pair("Redis", 12f),
            Pair("DynamoDB", 24f),
            Pair("PostgreSQL", 28f),
            Pair("Cassandra", 35f),
            Pair("MongoDB", 48f),
            Pair("Elasticsearch", 62f),
        )

        val colors = listOf(emeraldColor, cyanColor, tealColor, yellowColor, coralColor, purpleColor)

        return items.mapIndexed { idx, pair ->
            Dataset(
                entries = listOf(Entry(idx.toFloat(), pair.second, pair.first)),
                label = pair.first,
                color = colors[idx],
            )
        }
    }

    // 4. Donut/Pie: Cuota de Mercado de Navegadores Web
    fun generateBrowserSharePieData(): List<Dataset> {
        val entries = listOf(
            Entry(1f, 52f, "Chrome"),
            Entry(2f, 24f, "Safari"),
            Entry(3f, 14f, "Firefox"),
            Entry(4f, 10f, "Edge/Otros"),
        )
        return listOf(Dataset(entries = entries))
    }

    // 5. Stacked Bar: Ingresos por Canal de Venta
    fun generateSalesChannelStackedData(): List<StackedBarEntry> {
        val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago")
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

    // 6. Candlestick: Cotización KORA/USD
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

    // 7. Radar Data: Habilidades de Desarrollador Mobile
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

    // 8. Combined Data: Usuarios Activos vs Conversiones
    fun generateTrafficCombinedData(): List<Dataset> {
        val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val activeUsers = listOf(45f, 62f, 58f, 80f, 92f, 75f, 68f)
        val conversions = listOf(32f, 48f, 42f, 65f, 78f, 58f, 52f)

        val barEntries = days.mapIndexed { idx, d -> Entry(idx.toFloat(), activeUsers[idx], d) }
        val lineEntries = days.mapIndexed { idx, d -> Entry(idx.toFloat(), conversions[idx], d) }

        return listOf(
            Dataset(entries = barEntries, label = "Usuarios", color = lightPurpleColor),
            Dataset(entries = lineEntries, label = "Conversiones", color = yellowColor, lineWidth = 3f),
        )
    }

    // 9. Bubble Data: Proyectos (Riesgo vs Retorno vs Presupuesto)
    fun generateProjectRiskBubbleData(): List<BubbleEntry> {
        val projects = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7")
        val risks = listOf(15f, 28f, 42f, 58f, 68f, 78f, 88f)
        val returns = listOf(35f, 62f, 48f, 85f, 55f, 92f, 70f)
        val budgets = listOf(16f, 24f, 20f, 30f, 22f, 32f, 26f)
        val colors = listOf(emeraldColor, blueColor, cyanColor, yellowColor, lightPurpleColor, coralColor, purpleColor)

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
}
