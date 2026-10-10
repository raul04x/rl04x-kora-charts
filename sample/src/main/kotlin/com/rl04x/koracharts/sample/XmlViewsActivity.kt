package com.rl04x.koracharts.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
        val channelData = SampleData.generateSalesChannelStackedData()
        val cryptoData = SampleData.generateCryptoCandlestickData()
        val devRadarData = SampleData.generateDeveloperRadarData()
        val userTrafficData = SampleData.generateTrafficCombinedData()
        val riskData = SampleData.generateProjectRiskBubbleData()

        binding.lineChartView.setData(expensesData)
        binding.barChartView.setData(categoryData)
        binding.horizontalBarChartView.setData(latencyData)
        binding.pieChartView.setData(browserData)
        binding.stackedBarChartView.setData(channelData)
        binding.stackedBarChartView.onBarSelectedListener = { entry ->
            if (entry != null) {
                binding.card5.setBadge("Selected: ${entry.label}")
                binding.card5.setSubtitle("${entry.label}: Total €${entry.values.sum().toInt()}")
            } else {
                binding.card5.setBadge(null)
                binding.card5.setSubtitle("Accumulated breakdown (€)")
            }
        }
        binding.candlestickChartView.setData(cryptoData)
        binding.radarChartView.setData(devRadarData)
        binding.combinedChartView.setData(userTrafficData)
        binding.bubbleChartView.setData(riskData)
    }
}
