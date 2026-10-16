# 📊 Kora Charts

[![](https://jitpack.io/v/raul04x/rl04x-kora-charts.svg)](https://jitpack.io/#raul04x/raul04x-kora-charts)

**Kora Charts** is a modern, high-performance, themeable charting library for Android built natively
with **Jetpack Compose** and **Android Views**. It provides 9 chart types, zero-allocation Canvas
renderers, interactive 2D Zoom & Pan gestures, Glassmorphic overlays, mathematical axis scaling
(*Nice Numbers*), touch scrubbing, and complete Light & Dark theme customization.

---

## 🌟 Key Features

- **📐 Cartesian Plane Origin $(0, 0)$ & Flat Base Bars:**
    - Strict Cartesian coordinate system originating at $(0,0)$ with $0 \le x \le M$
      and $0 \le y \le N$. Points with $x > 0$ are visibly displaced from origin $X=0$.
    - Vertical bar charts render flat bottom bases anchored at $Y=0$ with top-only rounded corners
      and individual per-bar custom colors (`entry.color`).

- **⚡ Zero-Allocation Canvas Engine:**
    - Re-architected modular sub-renderers (`AxisGridRenderer`, `LegendRenderer`,
      `GlassTooltipRenderer`) with pre-allocated buffer recycling for zero Garbage Collection (GC)
      pressure at 60/120 FPS.

- **🍩 EVEN_ODD Donut Geometry & System Locale Currency Formatting:**
    - Closed path ring rendering with `FillType.EVEN_ODD` for 100% Skia GPU compatibility
      and $360^\circ$ full circle / single-entry safety.
    - Automatic system locale number formatting (`Locale.getDefault()`) supporting currency prefixes
      (`$`, `€`, `£`, `MXN $`) and suffixes.
    - Compact callouts for small slices show only the category and percentage (for example,
      `"Odontolo…: 4%"`), preserving the percentage when space is limited.
    - Selecting a slice shows its full category name, formatted value, and percentage in the
      tooltip; Compose and Views also expose selection-change callbacks.
    - Slice touch detection shares the renderer's layout, so hit testing accounts for the legend,
      chart margins, donut hole, and the offset of a selected slice.

- **🪟 Translucent Glass Tooltips:**
    - Glass badges featuring 85% translucency (`0xD9`) and clean left-aligned multi-line (`\n`) text
      formatting.
    - Automatic Stacked Bar breakdown cards detailing individual segment series names, segment
      values, and total sums.

- **📊 9 Chart Types Supported:**
    1. **Line Chart** (`KoraLineChart`) — Smooth bezier curves with gradient fills, node points, and
       crosshair highlight lines.
    2. **Bar Chart** (`KoraBarChart`) — Vertical rounded bars with $Y = 0$ flat-anchored baselines,
       top-only rounded corners, individual per-entry colors, and highlight borders.
    3. **Horizontal Bar Chart** (`KoraHorizontalBarChart`) — Horizontal progress bars with track
       backgrounds, value labels, and touch selection.
    4. **Donut & Pie Chart** (`KoraPieChart`) — Donut chart with true ring rendering, Google WCAG
       contrast text, auto-scaling center hole text, and smart bounds callouts.
    5. **Stacked Bar Chart** (`KoraStackedBarChart`) — Multi-segment stacked bars supporting 480+
       entries (e.g. credit loan schedules), top-only rounded corners, multi-row legend wrapping,
       and segment breakdown cards.
    6. **Candlestick Chart** (`KoraCandlestickChart`) — Financial candlestick chart with high/low
       wicks, bullish/bearish candle bodies, and 2D Zoom/Pan.
    7. **Radar / Spider Chart** (`KoraRadarChart`) — Multi-axis polygon web chart comparing datasets
       across skill categories.
    8. **Combined Chart** (`KoraCombinedChart`) — Dual-layer chart overlaying vertical bars and line
       curves with node dots and subpixel 1-to-1 alignment.
    9. **Bubble Scatter Plot** (`KoraBubbleChart`) — Translucent 2D Euclidean scatter plot with 2D
       Zoom/Pan and top/lateral headroom clipping immunity.

- **🔢 Mathematical Axis Scaling & Nice Numbers (`AxisTickCalculator`):**
    - Automatically calculates clean, whole-number axis intervals
      ($1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, \dots$).
    - Smart stride sampling and trigonometric centering for X-axis labels at bar column centers
      `(cx, baselineY + 12dp)`.

- **🎯 Key Value Indicators (Min, Max & Average Lines):**
    - Automatic floating badges for maximum and minimum entries (`▲ Max`, `▼ Min`).
    - Dashed horizontal line indicating chart average (`Avg`).

- **👆 Interactive Touch Scrubbing, Tap-to-Toggle & 2D Gestures:**
    - Continuous drag gesture (`detectDragGestures`) updating selection crosshairs and tooltips in
      real-time.
    - Multi-touch 2D Zoom & Pan gestures with pixel-accurate X-axis scrolling and `nearestLineEntry`
      touch precision.

- **📐 Reference Lines & Target Zones:**
    - **Threshold Lines (`ReferenceLine`):** Dashed horizontal threshold lines with custom labels
      and colors.
    - **Target Zones (`TargetZone`):** Translucent alert or goal bands across Y-axis ranges.

- **🔤 Smart Number Formatting & Currency:**
    - Automatic compact number formatting (`1.5K`, `2.4M`, `1B`).
    - Customizable value prefixes (`$`, `€`) and suffixes (`/mo`, `Mbps`).

- **⚖️ Dual Y-Axis Support:**
    - Secondary Y-axis on the right side for datasets configured with
      `dataset.useSecondaryAxis = true`.

- **🎨 Smart Contrast Text & Per-Item Styling:**
    - Automatic **Google WCAG 2.1 Relative Luminance** text contrast calculation for Pie and Stacked
      Bar labels.
    - Individual entry colors (`entry.color`) and custom font colors (`entry.textColor`).

- **🏷️ Multi-Row Word-Wrap Legend:**
    - Automatic horizontal legend wrapping into multiple rows to fit any number of dataset series.

- **♿ Screen Reader Accessibility (TalkBack):**
    - Built-in Compose `semantics` (`Role.Image`) and dynamic Android View `contentDescription`
      summaries in English.

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
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-compose:1.0.1")

    // Core charting engine
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-core:1.0.1")

    // For Android Views XML support (optional)
    implementation("com.github.raul04x.rl04x-kora-charts:rl04x-kora-views:1.0.1")
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

### 2. Donut Chart with Currency Formatting & Automatic Contrast

```kotlin
val pieDataset = Dataset(
    entries = listOf(
        Entry(0f, 2308f, "Sueldo", color = "#10B981".toColorInt()),
        Entry(1f, 23.3f, "Otros", color = "#F87171".toColorInt())
    )
)

val config = ChartConfig(
    style = KoraChartStyle.oledDark(),
    valuePrefix = "€",
    centerTitle = "Total",
    showLegend = false
)

KoraPieChart(
    datasets = listOf(pieDataset),
    config = config,
    holeRadius = 0.58f,
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
