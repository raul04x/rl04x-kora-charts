package com.rl04x.koracharts.sample

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.rl04x.koracharts.compose.KoraBarChart
import com.rl04x.koracharts.compose.KoraBubbleChart
import com.rl04x.koracharts.compose.KoraCandlestickChart
import com.rl04x.koracharts.compose.KoraChartCard
import com.rl04x.koracharts.compose.KoraCombinedChart
import com.rl04x.koracharts.compose.KoraHorizontalBarChart
import com.rl04x.koracharts.compose.KoraLineChart
import com.rl04x.koracharts.compose.KoraPieChart
import com.rl04x.koracharts.compose.KoraRadarChart
import com.rl04x.koracharts.compose.KoraStackedBarChart
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.KoraChartStyle
import com.rl04x.koracharts.sample.databinding.ActivityXmlViewsGalleryBinding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DashboardScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    // 0 = Menu 1: Compose Gallery, 1 = Menu 2: XML Views Gallery
    var selectedMenu by remember { mutableIntStateOf(0) }
    // 0 = Midnight Dark, 1 = OLED Deep Dark, 2 = Light
    var themeMode by remember { mutableIntStateOf(0) }
    var enableZoom by remember { mutableStateOf(true) }
    var useNiceTicks by remember { mutableStateOf(true) }
    var selectedHeightDp by remember { mutableIntStateOf(220) }

    val chartStyle = remember(themeMode) {
        when (themeMode) {
            0 -> KoraChartStyle.dark()
            1 -> KoraChartStyle.oledDark()
            else -> KoraChartStyle.light()
        }
    }

    val chartConfig = remember(chartStyle, enableZoom, useNiceTicks) {
        ChartConfig(
            style = chartStyle,
            enableZoom = enableZoom,
            useNiceTicks = useNiceTicks,
            forceIntegerTicks = true,
            showGrid = true,
            showAxisLabels = true,
            showHighlightLine = true,
        )
    }

    val expensesConfig = remember(chartConfig) {
        chartConfig.copy(
            showMinMaxBadges = true,
            showAverageLine = true,
            compactNumberFormatting = true,
            valuePrefix = "€",
            referenceLines = listOf(
                com.rl04x.koracharts.core.model.ReferenceLine(
                    value = 1500f,
                    label = "Budget €1.5k",
                    color = "#EF4444".toColorInt(),
                ),
            ),
            targetZones = listOf(
                com.rl04x.koracharts.core.model.TargetZone(
                    minY = 500f,
                    maxY = 1200f,
                    label = "Target Zone",
                    color = "#10B981".toColorInt(),
                    fillAlpha = 0.12f,
                ),
            ),
        )
    }

    val (expensesData, _) = remember { SampleData.generateMonthlyExpensesLineData() }
    val (revenueData, _) = remember { SampleData.generateRevenueThousandsLineData() }
    val (categoryData, _) = remember { SampleData.generateCategorySalesBarData() }
    val latencyData = remember { SampleData.generateDatabaseLatencyHorizontalData() }
    val browserData = remember { SampleData.generateBrowserSharePieData() }
    val channelData = remember { SampleData.generateSalesChannelStackedData() }
    val cryptoData = remember { SampleData.generateCryptoCandlestickData() }
    val devRadarData = remember { SampleData.generateDeveloperRadarData() }
    val userTrafficData = remember { SampleData.generateTrafficCombinedData() }
    val riskData = remember { SampleData.generateProjectRiskBubbleData() }

    val outerBgColor = when (themeMode) {
        0 -> Color(0xFF0B0F19)
        1 -> Color(0xFF08090F)
        else -> Color(0xFFF1F5F9)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = outerBgColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Header Title
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "📊 Kora Charts Showcase",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(chartStyle.titleTextColor),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Interactive dashboard supporting Compose and XML Views",
                    fontSize = 13.sp,
                    color = Color(chartStyle.subtitleTextColor),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Menu Tabs: 1. Compose Gallery | 2. XML Views Gallery
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(chartStyle.cardBackgroundColor),
                border = BorderStroke(1.dp, Color(chartStyle.cardBorderColor)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedMenu == 0) Color(chartStyle.highlightLineColor) else Color.Transparent)
                            .clickable { selectedMenu = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🚀 Menu 1: Compose",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMenu == 0) Color.White else Color(chartStyle.labelTextColor),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedMenu == 1) Color(chartStyle.highlightLineColor) else Color.Transparent)
                            .clickable { selectedMenu = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🖼️ Menu 2: XML Views",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMenu == 1) Color.White else Color(chartStyle.labelTextColor),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Global Control Bar 1: Visual Theme Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Theme:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(chartStyle.labelTextColor),
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(chartStyle.cardBackgroundColor),
                    border = BorderStroke(1.dp, Color(chartStyle.cardBorderColor)),
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (themeMode == 0) Color(chartStyle.highlightLineColor) else Color.Transparent)
                                .clickable { themeMode = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "🌙 Midnight",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (themeMode == 0) Color.White else Color(chartStyle.labelTextColor),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (themeMode == 1) Color(chartStyle.highlightLineColor) else Color.Transparent)
                                .clickable { themeMode = 1 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "🌌 OLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (themeMode == 1) Color.White else Color(chartStyle.labelTextColor),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (themeMode == 2) Color(chartStyle.highlightLineColor) else Color.Transparent)
                                .clickable { themeMode = 2 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "☀️ Light",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (themeMode == 2) Color.White else Color(chartStyle.labelTextColor),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Global Control Bar 2: Interactive Options (Zoom + N-Ticks Math Scale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Options:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(chartStyle.labelTextColor),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Zoom & Pan Control
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(chartStyle.cardBackgroundColor),
                        border = BorderStroke(1.dp, Color(chartStyle.cardBorderColor)),
                        modifier = Modifier.clickable { enableZoom = !enableZoom },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (enableZoom) "🔍 Zoom: ON" else "🔍 Zoom: OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enableZoom) Color(chartStyle.badgeTextColor) else Color(
                                    chartStyle.labelTextColor
                                ),
                            )
                        }
                    }

                    // Math Scale / N-Ticks Control
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(chartStyle.cardBackgroundColor),
                        border = BorderStroke(1.dp, Color(chartStyle.cardBorderColor)),
                        modifier = Modifier.clickable { useNiceTicks = !useNiceTicks },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (useNiceTicks) "🔢 Scale: ON" else "🔢 Scale: OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (useNiceTicks) Color(chartStyle.badgeTextColor) else Color(
                                    chartStyle.labelTextColor
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Global Control Bar 3: Chart Height Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Chart Height:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(chartStyle.labelTextColor),
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(chartStyle.cardBackgroundColor),
                    border = BorderStroke(1.dp, Color(chartStyle.cardBorderColor)),
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(
                            180 to "📐 180dp",
                            240 to "📜 240dp",
                            320 to "🏗️ 320dp"
                        ).forEach { (hDp, title) ->
                            val isSelected = (selectedHeightDp == hDp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) Color(chartStyle.highlightLineColor) else Color.Transparent)
                                    .clickable { selectedHeightDp = hDp }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(chartStyle.labelTextColor),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMenu == 1) {
                // ==================== MENU 2: TRADITIONAL XML VIEWS GALLERY ====================
                AndroidView(
                    factory = { ctx ->
                        val inflater = LayoutInflater.from(ctx)
                        val binding = ActivityXmlViewsGalleryBinding.inflate(inflater)

                        binding.lineChartView.setData(expensesData, expensesConfig)
                        binding.barChartView.setData(categoryData, chartConfig)
                        binding.horizontalBarChartView.setData(latencyData, chartConfig)
                        binding.pieChartView.setData(
                            browserData,
                            chartConfig.copy(centerTitle = "52%", centerSubtitle = "Chrome")
                        )
                        binding.stackedBarChartView.setData(channelData, chartConfig)
                        binding.candlestickChartView.setData(cryptoData, chartConfig)
                        binding.radarChartView.setData(devRadarData, chartConfig)
                        binding.combinedChartView.setData(userTrafficData, chartConfig)
                        binding.bubbleChartView.setData(riskData, chartConfig)

                        binding.root
                    },
                    update = { view ->
                        val density = view.resources.displayMetrics.density
                        val heightPx = (selectedHeightDp * density).toInt()

                        val binding = ActivityXmlViewsGalleryBinding.bind(view)

                        // Update heights
                        binding.lineChartView.layoutParams =
                            binding.lineChartView.layoutParams.apply { height = heightPx }
                        binding.barChartView.layoutParams =
                            binding.barChartView.layoutParams.apply { height = heightPx }
                        binding.horizontalBarChartView.layoutParams =
                            binding.horizontalBarChartView.layoutParams.apply { height = heightPx }
                        binding.pieChartView.layoutParams =
                            binding.pieChartView.layoutParams.apply { height = heightPx }
                        binding.stackedBarChartView.layoutParams =
                            binding.stackedBarChartView.layoutParams.apply { height = heightPx }
                        binding.candlestickChartView.layoutParams =
                            binding.candlestickChartView.layoutParams.apply { height = heightPx }
                        binding.radarChartView.layoutParams =
                            binding.radarChartView.layoutParams.apply { height = heightPx }
                        binding.combinedChartView.layoutParams =
                            binding.combinedChartView.layoutParams.apply { height = heightPx }
                        binding.bubbleChartView.layoutParams =
                            binding.bubbleChartView.layoutParams.apply { height = heightPx }

                        // Apply theme style to cards
                        binding.card1.applyStyle(chartStyle)
                        binding.card2.applyStyle(chartStyle)
                        binding.card3.applyStyle(chartStyle)
                        binding.card4.applyStyle(chartStyle)
                        binding.card5.applyStyle(chartStyle)
                        binding.card6.applyStyle(chartStyle)
                        binding.card7.applyStyle(chartStyle)
                        binding.card8.applyStyle(chartStyle)
                        binding.card9.applyStyle(chartStyle)

                        // Update data & configs
                        binding.lineChartView.setData(expensesData, expensesConfig)
                        binding.barChartView.setData(categoryData, chartConfig)
                        binding.horizontalBarChartView.setData(latencyData, chartConfig)
                        binding.pieChartView.setData(
                            browserData,
                            chartConfig.copy(centerTitle = "52%", centerSubtitle = "Chrome")
                        )
                        binding.stackedBarChartView.setData(channelData, chartConfig)
                        binding.candlestickChartView.setData(cryptoData, chartConfig)
                        binding.radarChartView.setData(devRadarData, chartConfig)
                        binding.combinedChartView.setData(userTrafficData, chartConfig)
                        binding.bubbleChartView.setData(riskData, chartConfig)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // ==================== MENU 1: JETPACK COMPOSE GALLERY ====================

                // 1. Line Chart: Expenses Comparison
                KoraChartCard(
                    title = "Expenses Comparison",
                    subtitle = "Current Month vs Previous Month (€)",
                    badgeText = "-8.5% vs last",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraLineChart(
                        datasets = expensesData,
                        config = expensesConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1b. Line Chart: Annual Revenue
                KoraChartCard(
                    title = "Annual Revenue",
                    subtitle = "X and Y axes with N-Ticks mathematical scale",
                    badgeText = "N-Ticks Scale",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraLineChart(
                        datasets = revenueData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Bar Chart: Sales by Category
                KoraChartCard(
                    title = "Sales by Category",
                    subtitle = "Year 2025 (€k)",
                    badgeText = "Top: Toys",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraBarChart(
                        datasets = categoryData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Horizontal Bar Chart: Database Engine Latency
                KoraChartCard(
                    title = "Database Engine Latency",
                    subtitle = "Average response time (ms)",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraHorizontalBarChart(
                        datasets = latencyData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Donut/Pie Chart: Web Browsers
                KoraChartCard(
                    title = "Web Browsers",
                    subtitle = "Global market share (%)",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraPieChart(
                        datasets = browserData,
                        config = chartConfig.copy(
                            centerTitle = "52%",
                            centerSubtitle = "Chrome",
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Stacked Bar Chart: Sales Channels
                KoraChartCard(
                    title = "Sales Channels",
                    subtitle = "Accumulated breakdown (€k)",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraStackedBarChart(
                        entries = channelData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Candlestick Chart: KORA / USD Quote
                KoraChartCard(
                    title = "KORA / USD Quote",
                    subtitle = "Daily candles",
                    badgeText = "+8.5%",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraCandlestickChart(
                        candles = cryptoData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7. Radar Chart: Developer Skill Profile
                KoraChartCard(
                    title = "Developer Skill Profile",
                    subtitle = "Senior vs Junior assessment",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraRadarChart(
                        datasets = devRadarData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 8. Combined Chart: Active Users vs Conversions
                KoraChartCard(
                    title = "Active Users vs Conversions",
                    subtitle = "Bars: Users · Line: Conversions",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraCombinedChart(
                        datasets = userTrafficData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 9. Bubble Chart: Project Portfolio Matrix
                KoraChartCard(
                    title = "Project Portfolio Matrix",
                    subtitle = "X: Risk · Y: Return · Radius: Budget",
                    chartHeight = selectedHeightDp.dp,
                    style = chartStyle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    KoraBubbleChart(
                        bubbles = riskData,
                        config = chartConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
