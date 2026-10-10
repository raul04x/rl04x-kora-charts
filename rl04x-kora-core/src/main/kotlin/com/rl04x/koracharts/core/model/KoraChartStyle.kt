package com.rl04x.koracharts.core.model

import androidx.core.graphics.toColorInt

/**
 * Theme and styling configuration for Kora Charts.
 * Supports Light, Dark (Midnight), Deep Dark (OLED), and custom color configurations with glassmorphism presets.
 */
public data class KoraChartStyle(
    val cardBackgroundColor: Int = "#0F172A".toColorInt(),
    val cardBorderColor: Int = "#1E293B".toColorInt(),
    val gridColor: Int = "#1E293B".toColorInt(),
    val axisColor: Int = "#334155".toColorInt(),
    val labelTextColor: Int = "#94A3B8".toColorInt(),
    val titleTextColor: Int = "#F8FAFC".toColorInt(),
    val subtitleTextColor: Int = "#64748B".toColorInt(),
    val badgeBackgroundColor: Int = "#1E1B4B".toColorInt(),
    val badgeTextColor: Int = "#818CF8".toColorInt(),
    val tooltipBackgroundColor: Int = "#D91E293B".toColorInt(),
    val tooltipBorderColor: Int = "#38BDF8".toColorInt(),
    val tooltipTextColor: Int = "#FFFFFF".toColorInt(),
    val highlightLineColor: Int = "#38BDF8".toColorInt(),
    val trackBackgroundColor: Int = "#1E293B".toColorInt(),
) {
    public companion object {
        /** Midnight/Obsidian Dark Theme preset (Midnight Blue & Electric Cyan) */
        public fun dark(): KoraChartStyle = KoraChartStyle(
            cardBackgroundColor = "#0F172A".toColorInt(),
            cardBorderColor = "#1E293B".toColorInt(),
            gridColor = "#1E293B".toColorInt(),
            axisColor = "#334155".toColorInt(),
            labelTextColor = "#94A3B8".toColorInt(),
            titleTextColor = "#F8FAFC".toColorInt(),
            subtitleTextColor = "#64748B".toColorInt(),
            badgeBackgroundColor = "#1E1B4B".toColorInt(),
            badgeTextColor = "#818CF8".toColorInt(),
            tooltipBackgroundColor = "#D91E293B".toColorInt(),
            tooltipBorderColor = "#38BDF8".toColorInt(),
            tooltipTextColor = "#FFFFFF".toColorInt(),
            highlightLineColor = "#38BDF8".toColorInt(),
            trackBackgroundColor = "#1E293B".toColorInt(),
        )

        /** Deep Dark OLED Theme preset (Pitch Black for OLED/AMOLED screens) */
        public fun oledDark(): KoraChartStyle = KoraChartStyle(
            cardBackgroundColor = "#12131C".toColorInt(),
            cardBorderColor = "#222436".toColorInt(),
            gridColor = "#1C1E30".toColorInt(),
            axisColor = "#2D3048".toColorInt(),
            labelTextColor = "#A9B1D6".toColorInt(),
            titleTextColor = "#C0CAF5".toColorInt(),
            subtitleTextColor = "#7AA2F7".toColorInt(),
            badgeBackgroundColor = "#172E2B".toColorInt(),
            badgeTextColor = "#2DD4BF".toColorInt(),
            tooltipBackgroundColor = "#D91A1B26".toColorInt(),
            tooltipBorderColor = "#2DD4BF".toColorInt(),
            tooltipTextColor = "#FFFFFF".toColorInt(),
            highlightLineColor = "#2DD4BF".toColorInt(),
            trackBackgroundColor = "#1C1E30".toColorInt(),
        )

        /** Light Theme preset */
        public fun light(): KoraChartStyle = KoraChartStyle(
            cardBackgroundColor = "#FFFFFF".toColorInt(),
            cardBorderColor = "#E2E8F0".toColorInt(),
            gridColor = "#F1F5F9".toColorInt(),
            axisColor = "#E2E8F0".toColorInt(),
            labelTextColor = "#64748B".toColorInt(),
            titleTextColor = "#0F172A".toColorInt(),
            subtitleTextColor = "#94A3B8".toColorInt(),
            badgeBackgroundColor = "#D1FAE5".toColorInt(),
            badgeTextColor = "#059669".toColorInt(),
            tooltipBackgroundColor = "#E60F172A".toColorInt(),
            tooltipBorderColor = "#334155".toColorInt(),
            tooltipTextColor = "#FFFFFF".toColorInt(),
            highlightLineColor = "#14B8A6".toColorInt(),
            trackBackgroundColor = "#F1F5F9".toColorInt(),
        )
    }
}
