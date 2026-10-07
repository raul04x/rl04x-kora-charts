# 📊 Kora Charts

**Kora Charts** is a modern, high-performance, themeable charting library for Android built natively with **Jetpack Compose** and **Android Views**. It provides 9 chart types, interactive 2D Zoom & Pan gestures, Glassmorphic overlays, and complete Light & Dark theme customization.

---

## 🌟 Key Features

* **9 Chart Types Supported:**
  1. **Line Chart** (`KoraLineChart`) — Smooth bezier curves with gradient fills, node points, and crosshair highlight lines.
  2. **Bar Chart** (`KoraBarChart`) — Vertical rounded bars with individual bar selection and highlight borders.
  3. **Horizontal Bar Chart** (`KoraHorizontalBarChart`) — Horizontal progress bars with track backgrounds and value labels.
  4. **Donut & Pie Chart** (`KoraPieChart`) — Donut chart with auto-scaling center hole text and animated slice explosion.
  5. **Stacked Bar Chart** (`KoraStackedBarChart`) — Multi-segment stacked bars with top-only rounded corners and total value labels.
  6. **Candlestick Chart** (`KoraCandlestickChart`) — Financial candlestick chart with high/low wicks and bullish/bearish candle bodies.
  7. **Radar / Spider Chart** (`KoraRadarChart`) — Multi-axis polygon web chart comparing datasets across skill categories.
  8. **Combined Chart** (`KoraCombinedChart`) — Dual-layer chart overlaying vertical bars and line curves with node dots.
  9. **Bubble Scatter Plot** (`KoraBubbleChart`) — Translucent bubble scatter plot with variable positions, radii, and colors.

* **🎨 Theme System (`KoraChartStyle`):**
  * **Midnight Dark:** Rich slate navy & electric cyan (`KoraChartStyle.dark()`).
  * **OLED Pitch Black:** Pure dark for AMOLED/OLED screens (`KoraChartStyle.oledDark()`).
  * **Light Mode:** Clean, bright mint theme (`KoraChartStyle.light()`).
  * **Full Customization:** Override card background, grid lines, axis colors, text colors, badge colors, and tooltip borders.

* **🔍 Interactive 2D Zoom & Pan:**
  * Pinch-to-zoom in both X and Y dimensions (`1x` to `5x`).
  * Drag-to-pan with smooth boundaries and instant screen-to-data index mapping.

* **🔤 Smart X-Axis Label Formatting:**
  * Auto-truncation (`…`) for long text labels.
  * Auto-rotation (`-90°` vertical counter-clockwise) for long labels to eliminate overlapping.
  * Configurable label rotation angle (`xAxisLabelRotation`).

* **🎴 Card Container (`KoraChartCard`):**
  * Customizable card height (`chartHeight = 300.dp`).
  * Titles, subtitles, and status pill badges (`+12.4%`, `Top: Juguetes`).

---

## 📦 Installation & Setup

Add the dependencies to your module's `build.gradle.kts`:

```kotlin
dependencies {
    // Core charting engine & renderers
    implementation(project(":rl04x-kora-core"))

    // Jetpack Compose components
    implementation(project(":rl04x-kora-compose"))

    // Android Views (XML Layouts support)
    implementation(project(":rl04x-kora-views"))
}
```

---

## 🚀 Quick Start & Usage Examples

### 1. Line Chart in Jetpack Compose

```kotlin
val dataset = Dataset(
    entries = listOf(
        Entry(0f, 18f, "Jan"),
        Entry(1f, 45f, "Feb"),
        Entry(2f, 84f, "Mar")
    ),
    label = "Network Traffic",
    color = "#14B8A6".toColorInt(),
    lineWidth = 1.5f,
    pointRadius = 2.5f,
    isCurved = true,
    gradientFill = true
)

val config = ChartConfig(
    style = KoraChartStyle.dark(),
    enableZoom = true,
    showGrid = true,
    xAxisLabelRotation = -45f
)

KoraChartCard(
    title = "Network Traffic",
    subtitle = "Monthly performance (Mbps)",
    badgeText = "+12.4%",
    chartHeight = 240.dp,
    style = config.style
) {
    KoraLineChart(
        datasets = listOf(dataset),
        config = config,
        modifier = Modifier.fillMaxSize()
    )
}
```

### 2. Donut Chart with Center Hole Label

```kotlin
val pieDataset = Dataset(
    entries = listOf(
        Entry(1f, 52f, "Chrome"),
        Entry(2f, 24f, "Safari"),
        Entry(3f, 14f, "Firefox"),
        Entry(4f, 10f, "Edge")
    )
)

KoraPieChart(
    datasets = listOf(pieDataset),
    config = ChartConfig(
        style = KoraChartStyle.oledDark(),
        centerTitle = "52%",
        centerSubtitle = "Chrome"
    ),
    holeRadius = 0.55f,
    modifier = Modifier.height(240.dp)
)
```

### 3. Stacked Bar Chart with Custom Height

```kotlin
val stackedEntries = listOf(
    StackedBarEntry(
        x = 0f,
        values = listOf(25f, 18f, 12f),
        colors = listOf("#14B8A6".toColorInt(), "#F87171".toColorInt(), "#F59E0B".toColorInt()),
        label = "Jan"
    )
)

KoraStackedBarChart(
    entries = stackedEntries,
    config = ChartConfig(style = KoraChartStyle.light()),
    modifier = Modifier.height(280.dp)
)
```

---

## 🎨 Theme System Customization

You can define your own custom `KoraChartStyle`:

```kotlin
val customStyle = KoraChartStyle(
    cardBackgroundColor = "#0F172A".toColorInt(),
    cardBorderColor = "#1E293B".toColorInt(),
    gridColor = "#1E293B".toColorInt(),
    titleTextColor = "#F8FAFC".toColorInt(),
    subtitleTextColor = "#94A3B8".toColorInt(),
    badgeBackgroundColor = "#1E1B4B".toColorInt(),
    badgeTextColor = "#818CF8".toColorInt(),
    highlightLineColor = "#38BDF8".toColorInt(),
    tooltipBackgroundColor = "#1E293B".toColorInt(),
    tooltipBorderColor = "#38BDF8".toColorInt()
)
```

---

## 📄 License & Notice

**Kora Charts** is open-source software licensed under the **Apache License, Version 2.0**.

```text
Copyright 2026 Raul Oviedo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See the [LICENSE](LICENSE) and [NOTICE](NOTICE) files for full copyright and licensing details.
