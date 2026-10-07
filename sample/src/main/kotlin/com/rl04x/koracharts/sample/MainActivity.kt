package com.rl04x.koracharts.sample

import android.os.Bundle
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
                    label = "Presupuesto €1.5k",
                    color = android.graphics.Color.parseColor("#EF4444"),
                ),
            ),
            targetZones = listOf(
                com.rl04x.koracharts.core.model.TargetZone(
                    minY = 500f,
                    maxY = 1200f,
                    label = "Zona de Control",
                    color = android.graphics.Color.parseColor("#10B981"),
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
                    text = "📊 Kora Charts",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(chartStyle.titleTextColor),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dashboard interactivo con altura, temas y gestos personalizables",
                    fontSize = 13.sp,
                    color = Color(chartStyle.subtitleTextColor),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Bar: 1. Selector de Tema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tema visual:",
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
                                text = "☀️ Claro",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (themeMode == 2) Color.White else Color(chartStyle.labelTextColor),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Control Bar: 2. Fila de Botones Interactivos (Zoom + Escala)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Opciones:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(chartStyle.labelTextColor),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Control de Zoom & Pan
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

                    // Control de Escala Matemática / Nice Ticks
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
                                text = if (useNiceTicks) "🔢 Escala: ON" else "🔢 Escala: OFF",
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

            // Selector de Altura del Gráfico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Altura del gráfico:",
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

            // 1. Line Chart: Comparativa de Gastos (Mes Actual vs Mes Anterior)
            KoraChartCard(
                title = "Comparativa de Gastos",
                subtitle = "Mes Actual vs Mes Anterior (€)",
                badgeText = "-8.5% vs anterior",
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

            // 1b. Line Chart: Ingresos Anuales (Escala en Miles)
            KoraChartCard(
                title = "Ingresos Anuales (Escala en Miles)",
                subtitle = "Ejes X e Y con saltos matemáticos de 500k/1000k",
                badgeText = "Escala N-Ticks",
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

            // 2. Bar Chart: Ventas por Categoría
            KoraChartCard(
                title = "Ventas por Categoría",
                subtitle = "Año 2025 (€k)",
                badgeText = "Top: Juguetes",
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

            // 3. Horizontal Bar Chart: Latencia BD
            KoraChartCard(
                title = "Latencia de Motores BD",
                subtitle = "Tiempo de respuesta medio (ms)",
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

            // 4. Donut/Pie Chart: Navegadores
            KoraChartCard(
                title = "Navegadores Web",
                subtitle = "Cuota de mercado global (%)",
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

            // 5. Stacked Bar Chart: Canales de Venta
            KoraChartCard(
                title = "Canales de Venta",
                subtitle = "Desglose acumulado (€k)",
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

            // 6. Candlestick Chart: KORA / USD
            KoraChartCard(
                title = "Cotización KORA / USD",
                subtitle = "Velas diarias",
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

            // 7. Radar Chart: Perfil Dev
            KoraChartCard(
                title = "Perfil de Desarrollador",
                subtitle = "Evaluación Senior vs Junior",
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

            // 8. Combined Chart: Tráfico y Conversiones
            KoraChartCard(
                title = "Usuarios vs Conversiones",
                subtitle = "Barras: Usuarios · Línea: Conversiones",
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

            // 9. Bubble Chart: Portafolio de Proyectos
            KoraChartCard(
                title = "Matriz de Proyectos",
                subtitle = "Eje X: Riesgo · Eje Y: Retorno · Radio: Presupuesto",
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
