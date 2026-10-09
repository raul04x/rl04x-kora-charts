# 📊 Kora Charts

[![](https://jitpack.io/v/raul04x/rl04x-kora-charts.svg)](https://jitpack.io/#raul04x/rl04x-kora-charts)

**Kora Charts** is a modern, high-performance, themeable charting library for Android built natively with **Jetpack Compose** and **Android Views**. It provides 9 chart types, interactive 2D Zoom & Pan gestures, Glassmorphic overlays, mathematical axis scaling (*Nice Numbers*), touch scrubbing, and complete Light & Dark theme customization.

---

## 🌟 Key Features

- **9 Chart Types Supported:**
  1. **Line Chart** (`KoraLineChart`) — Smooth bezier curves with gradient fills, node points, and crosshair highlight lines.
  2. **Bar Chart** (`KoraBarChart`) — Vertical rounded bars with individual bar selection and highlight borders.
  3. **Horizontal Bar Chart** (`KoraHorizontalBarChart`) — Horizontal progress bars with track backgrounds and value labels.
  4. **Donut & Pie Chart** (`KoraPieChart`) — Donut chart with true ring rendering, Google WCAG contrast text, and auto-scaling center hole text.
  5. **Stacked Bar Chart** (`KoraStackedBarChart`) — Multi-segment stacked bars with top-only rounded corners, segment labels, and custom text colors.
  6. **Candlestick Chart** (`KoraCandlestickChart`) — Financial candlestick chart with high/low wicks and bullish/bearish candle bodies.
  7. **Radar / Spider Chart** (`KoraRadarChart`) — Multi-axis polygon web chart comparing datasets across skill categories.
  8. **Combined Chart** (`KoraCombinedChart`) — Dual-layer chart overlaying vertical bars and line curves with node dots.
  9. **Bubble Scatter Plot** (`KoraBubbleChart`) — Translucent bubble scatter plot with variable positions, radii, and colors.

- **🔢 Mathematical Axis Scaling & Nice Numbers (`AxisTickCalculator`):**
  - Automatically calculates clean, whole-number axis intervals ($1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, \dots$).
  - Supports custom explicit step sizes via `xAxisStep` and `yAxisStep`.

- **🎯 Key Value Indicators (Min, Max & Average Lines):**
  - Automatic floating badges for maximum and minimum entries (`▲ Max`, `▼ Min`).
  - Dashed horizontal line indicating chart average (`Avg`).

- **👆 Interactive Touch Scrubbing & Drag Inspection:**
  - Continuous drag gesture (`detectDragGestures`) across chart canvas updating selection crosshairs and tooltips in real-time.

- **📐 Reference Lines & Target Zones:**
  - **Threshold Lines (`ReferenceLine`):** Dashed horizontal threshold lines with custom labels and colors.
  - **Target Zones (`TargetZone`):** Translucent alert or goal bands across Y-axis ranges.

- **🔤 Smart Number Formatting & Currency:**
  - Automatic compact number formatting (`1.5K`, `2.4M`, `1B`).
  - Customizable value prefixes (`$`, `€`) and suffixes (`/mo`, `Mbps`).

- **⚖️ Dual Y-Axis Support:**
  - Secondary Y-axis on the right side for datasets configured with `dataset.useSecondaryAxis = true`.

- **🎨 Smart Contrast Text & Per-Item Styling:**
  - Automatic **Google WCAG 2.1 Relative Luminance** text contrast calculation for Pie and Stacked Bar labels.
  - Individual entry colors (`entry.color`) and custom font colors (`entry.textColor`).

- **🏷️ Chart Legend:**
  - Automatic legend rendering (`• Current Month`, `• Previous Month`) when `config.showLegend = true`.

- **♿ TalkBack Accessibility:**
  - Built-in Compose `semantics` providing structured screen reader summaries.

- **🎨 Theme System (`KoraChartStyle`):**
  - **Midnight Dark:** Rich slate navy & electric cyan (`KoraChartStyle.dark()`).
  - **OLED Pitch Black:** Pure dark for AMOLED/OLED screens (`KoraChartStyle.oledDark()`).
  - **Light Mode:** Clean, bright mint theme (`KoraChartStyle.light()`).

---

## 📦 Installation & Setup

### Step 1. Add the JitPack repository to your `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2. Add the dependencies to your `app/build.gradle.kts`

```kotlin
dependencies {
    // For Jetpack Compose components
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-compose:1.0.0-alpha03")

    // Core charting engine
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-core:1.0.0-alpha03")

    // For Android Views XML support (optional)
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-views:1.0.0-alpha03")
}
```

---

## 🚀 Quick Start & Usage Examples

### 1. Expenses Comparison Line Chart with Target Zones & Thresholds

```kotlin
val currentMonth = Dataset(
    entries = listOf(
        Entry(1f, 120f, "Day 1"),
        Entry(5f, 580f, "Day 5"),
        Entry(15f, 1146f, "Day 15"),
        Entry(30f, 1750f, "Day 30")
    ),
    label = "Current Month",
    color = "#14B8A6".toColorInt(),
    isCurved = true,
    gradientFill = true
)

val previousMonth = Dataset(
    entries = listOf(
        Entry(1f, 150f, "Day 1"),
        Entry(5f, 690f, "Day 5"),
        Entry(15f, 1310f, "Day 15"),
        Entry(30f, 1920f, "Day 30")
    ),
    label = "Previous Month",
    color = "#818CF8".toColorInt(),
    isCurved = true
)

val config = ChartConfig(
    style = KoraChartStyle.dark(),
    useNiceTicks = true,
    showMinMaxBadges = true,
    showAverageLine = true,
    compactNumberFormatting = true,
    valuePrefix = "€",
    referenceLines = listOf(
        ReferenceLine(value = 1500f, label = "Budget €1.5k", color = "#EF4444".toColorInt())
    ),
    targetZones = listOf(
        TargetZone(minY = 500f, maxY = 1200f, label = "Target Zone", color = "#10B981".toColorInt())
    )
)

KoraChartCard(
    title = "Expenses Comparison",
    subtitle = "Current Month vs Previous Month (€)",
    badgeText = "-8.5%",
    chartHeight = 240.dp,
    style = config.style
) {
    KoraLineChart(
        datasets = listOf(currentMonth, previousMonth),
        config = config,
        modifier = Modifier.fillMaxSize()
    )
}
```

### 2. Donut Chart with Custom Entry Colors & Automatic Contrast

```kotlin
val pieDataset = Dataset(
    entries = listOf(
        Entry(1f, 52f, "Chrome", color = "#F59E0B".toColorInt()), // Yellow background -> auto dark text
        Entry(2f, 24f, "Safari", color = "#3B82F6".toColorInt(), textColor = Color.WHITE),
        Entry(3f, 14f, "Firefox", color = "#8B5CF6".toColorInt()),
        Entry(4f, 10f, "Edge", color = "#F87171".toColorInt())
    )
)

KoraPieChart(
    datasets = listOf(pieDataset),
    config = ChartConfig(
        style = KoraChartStyle.oledDark(),
        centerTitle = "Total",
        centerSubtitle = "1,146.78"
    ),
    holeRadius = 0.55f,
    modifier = Modifier.height(240.dp)
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
