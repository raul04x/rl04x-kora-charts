package com.rl04x.koracharts.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.RadarDataSet
import com.rl04x.koracharts.core.model.StackedBarEntry
import com.rl04x.koracharts.core.renderer.BubbleRenderer
import com.rl04x.koracharts.core.renderer.CandlestickRenderer
import com.rl04x.koracharts.core.renderer.CombinedRenderer
import com.rl04x.koracharts.core.renderer.HorizontalBarRenderer
import com.rl04x.koracharts.core.renderer.RadarRenderer
import com.rl04x.koracharts.core.renderer.StackedBarRenderer

/**
 * Horizontal Bar Chart composable.
 */
@Composable
public fun KoraHorizontalBarChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { HorizontalBarRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = datasets,
                config = config,
                progress = progress.value,
            )
        }
    }
}

/**
 * Stacked Bar Chart composable.
 */
@Composable
public fun KoraStackedBarChart(
    entries: List<StackedBarEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { StackedBarRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(entries) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = entries,
                config = config,
                progress = progress.value,
            )
        }
    }
}

/**
 * Financial Candlestick Chart composable.
 */
@Composable
public fun KoraCandlestickChart(
    candles: List<CandlestickEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { CandlestickRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(candles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = candles,
                config = config,
                progress = progress.value,
            )
        }
    }
}

/**
 * Radar/Spider Web Polygon Chart composable.
 */
@Composable
public fun KoraRadarChart(
    datasets: List<RadarDataSet>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { RadarRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = datasets,
                config = config,
                progress = progress.value,
            )
        }
    }
}

/**
 * Combined Bar + Line Overlay Chart composable.
 */
@Composable
public fun KoraCombinedChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { CombinedRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = datasets,
                config = config,
                progress = progress.value,
            )
        }
    }
}

/**
 * Bubble Scatter Plot Chart composable.
 */
@Composable
public fun KoraBubbleChart(
    bubbles: List<BubbleEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
) {
    val renderer = remember { BubbleRenderer() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(bubbles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(
                canvas = canvas.nativeCanvas,
                width = size.width,
                height = size.height,
                data = bubbles,
                config = config,
                progress = progress.value,
            )
        }
    }
}
