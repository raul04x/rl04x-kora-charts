package com.rl04x.koracharts.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.sample.databinding.ActivityXmlViewsGalleryBinding

/**
 * Traditional AppCompatActivity demonstrating Kora Charts inflated from XML layout files.
 */
class XmlViewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityXmlViewsGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXmlViewsGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupXmlCharts()
    }

    private fun setupXmlCharts() {
        val (expensesData, _) = SampleData.generateMonthlyExpensesLineData()
        val (categoryData, _) = SampleData.generateCategorySalesBarData()
        val latencyData = SampleData.generateDatabaseLatencyHorizontalData()
        val browserData = SampleData.generateBrowserSharePieData()
        val incomeData = SampleData.generateIncomeDistributionDonutData()
        val channelData = SampleData.generateSalesChannelStackedData()
        val cryptoData = SampleData.generateCryptoCandlestickData()
        val devRadarData = SampleData.generateDeveloperRadarData()
        val userTrafficData = SampleData.generateTrafficCombinedData()
        val riskData = SampleData.generateProjectRiskBubbleData()

        binding.lineChartView.setData(expensesData)
        binding.lineChartView.onPointSelectedListener = { x, y ->
            binding.card1.setSubtitle("Selected: Day ${x.toInt()} · Expense €${y.toInt()}")
        }

        binding.barChartView.setData(categoryData)
        binding.barChartView.onBarSelectedListener = { entry ->
            binding.card2.setSubtitle(entry?.label?.let { "Selected: ${it.substringBefore('\n')}" }
                ?: "Sales by Category")
        }

        binding.horizontalBarChartView.setData(latencyData)
        binding.horizontalBarChartView.onBarSelectedListener = { dataset ->
            binding.card3.setSubtitle(dataset?.label?.let { "$it: ${dataset.entries.firstOrNull()?.y?.toInt()} ms" }
                ?: "Average response time (ms)")
        }

        binding.pieChartView.setData(browserData)
        val incomeConfig = ChartConfig(
            valuePrefix = "€",
            centerTitle = "Total",
            showLegend = false,
        )
        binding.incomePieChartView.setData(incomeData, incomeConfig)

        binding.stackedBarChartView.setData(channelData)
        binding.stackedBarChartView.onBarSelectedListener = { entry ->
            if (entry != null) {
                binding.card5.setBadge("Selected: ${entry.label}")
                binding.card5.setSubtitle("${entry.label}: Total €${entry.values.sum().toInt()}")
            } else {
                binding.card5.setBadge(null)
                binding.card5.setSubtitle("Fixed €222/mo · Principal vs Interest")
            }
        }

        binding.candlestickChartView.setData(cryptoData)
        binding.candlestickChartView.onCandleSelectedListener = { candle ->
            if (candle != null) {
                binding.card6.setBadge("Close: $${candle.close.toInt()}")
                binding.card6.setSubtitle("${candle.label}: Open $${candle.open.toInt()} · High $${candle.high.toInt()} · Low $${candle.low.toInt()} · Close $${candle.close.toInt()}")
            } else {
                binding.card6.setBadge(null)
                binding.card6.setSubtitle("Daily candles")
            }
        }

        binding.radarChartView.setData(devRadarData)

        binding.combinedChartView.setData(userTrafficData)
        binding.combinedChartView.onPointSelectedListener = { x, y ->
            binding.card8.setSubtitle("Selected: Day ${x.toInt()} · Value ${y.toInt()}")
        }

        binding.bubbleChartView.setData(riskData)
        binding.bubbleChartView.onBubbleSelectedListener = { bubble ->
            if (bubble != null) {
                binding.card9.setBadge("Selected: ${bubble.label}")
                binding.card9.setSubtitle("${bubble.label}: Risk ${bubble.x.toInt()} · Return ${bubble.y.toInt()} · Budget €${bubble.radiusDp.toInt()}k")
            } else {
                binding.card9.setBadge(null)
                binding.card9.setSubtitle("X: Risk · Y: Return · Radius: Budget")
            }
        }
    }
}
