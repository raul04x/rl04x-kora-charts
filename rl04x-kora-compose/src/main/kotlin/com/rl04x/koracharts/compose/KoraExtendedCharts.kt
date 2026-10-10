package com.rl04x.koracharts.compose

import android.content.res.Resources
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.rl04x.koracharts.core.model.BubbleEntry
import com.rl04x.koracharts.core.model.CandlestickEntry
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
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
 * Stacked Bar Chart composable supporting touch selection, labels, and 2D Zoom & Pan gestures.
 */
@Composable
public fun KoraStackedBarChart(
    entries: List<StackedBarEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onBarSelected: ((entry: StackedBarEntry?) -> Unit)? = null,
) {
    val renderer = remember { StackedBarRenderer() }
    val progress = remember { Animatable(0f) }
    var selectedBar by remember {
        mutableStateOf<StackedBarEntry?>(
            entries.firstOrNull { it.x == config.selectedEntry?.x || it.label == config.selectedEntry?.label }
        )
    }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }
    var zoomScaleY by remember { mutableFloatStateOf(config.zoomScaleY) }
    var panOffsetY by remember { mutableFloatStateOf(config.panOffsetY) }

    LaunchedEffect(config.selectedEntry) {
        selectedBar =
            entries.firstOrNull { it.x == config.selectedEntry?.x || it.label == config.selectedEntry?.label }
    }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX, config.zoomScaleY, config.panOffsetY) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
        zoomScaleY = config.zoomScaleY
        panOffsetY = config.panOffsetY
    }

    LaunchedEffect(entries) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val selectedEntry = remember(selectedBar) {
        selectedBar?.let { Entry(it.x, it.values.sum(), it.label) }
    }

    val activeConfig =
        remember(config, selectedEntry, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
            config.copy(
                selectedEntry = selectedEntry,
                zoomScaleX = zoomScaleX,
                panOffsetX = panOffsetX,
                zoomScaleY = zoomScaleY,
                panOffsetY = panOffsetY,
            )
        }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(entries, config, zoomScaleX, panOffsetX, selectedBar) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val leftPadding =
                            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                        val rightPadding = config.paddingDp * density + 10f * density
                        val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                        val entryCount = entries.size
                        if (entryCount > 0 && drawWidth > 0f) {
                            val effectiveZoomX = maxOf(1f, zoomScaleX)
                            val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
                            val relativeX = offset.x - leftPadding + panOffsetX
                            val clickedIdx = (relativeX / scaledSlotWidth).toInt()

                            if (clickedIdx in 0 until entryCount) {
                                val tapped = entries[clickedIdx]
                                if (selectedBar == tapped) {
                                    // Tapping same bar closes tooltip
                                    selectedBar = null
                                    onBarSelected?.invoke(null)
                                } else {
                                    selectedBar = tapped
                                    onBarSelected?.invoke(tapped)
                                }
                            } else {
                                // Tapping outside closes tooltip
                                selectedBar = null
                                onBarSelected?.invoke(null)
                            }
                        }
                    }
                }
                .pointerInput(entries, config, zoomScaleX, panOffsetX) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding = config.paddingDp * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val entryCount = entries.size
                            if (entryCount > 0 && drawWidth > 0f) {
                                val effectiveZoomX = maxOf(1f, zoomScaleX)
                                val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
                                val relativeX = offset.x - leftPadding + panOffsetX
                                val clickedIdx =
                                    (relativeX / scaledSlotWidth).toInt()
                                        .coerceIn(0, entryCount - 1)
                                val tapped = entries.getOrNull(clickedIdx)
                                selectedBar = tapped
                                tapped?.let { onBarSelected?.invoke(it) }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding = config.paddingDp * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val entryCount = entries.size
                            if (entryCount > 0 && drawWidth > 0f) {
                                val effectiveZoomX = maxOf(1f, zoomScaleX)
                                val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
                                val relativeX = change.position.x - leftPadding + panOffsetX
                                val clickedIdx =
                                    (relativeX / scaledSlotWidth).toInt()
                                        .coerceIn(0, entryCount - 1)
                                val tapped = entries.getOrNull(clickedIdx)
                                if (tapped != selectedBar) {
                                    selectedBar = tapped
                                    tapped?.let { onBarSelected?.invoke(it) }
                                }
                            }
                        }
                    )
                }
                .pointerInput(entries, config) {
                    if (config.enableZoom) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScaleX = (zoomScaleX * zoom).coerceIn(1f, 5f)
                            zoomScaleY = (zoomScaleY * zoom).coerceIn(1f, 5f)

                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding = config.paddingDp * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val maxPanPixels =
                                (drawWidth * zoomScaleX - drawWidth).coerceAtLeast(0f)
                            panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPanPixels)

                            val maxTotal =
                                entries.maxOfOrNull { it.values.sum() }?.coerceAtLeast(1f) ?: 100f
                            val visibleRangeY = maxTotal / zoomScaleY
                            val maxPanY = (maxTotal - visibleRangeY).coerceAtLeast(0f)
                            val deltaDataY = pan.y / size.height.toFloat() * visibleRangeY
                            panOffsetY = (panOffsetY + deltaDataY).coerceIn(0f, maxPanY)
                        }
                    }
                },
        ) {
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    data = entries,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
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
