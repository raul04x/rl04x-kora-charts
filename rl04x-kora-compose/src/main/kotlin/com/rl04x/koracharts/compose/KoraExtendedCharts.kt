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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.rl04x.koracharts.core.engine.ChartEngine
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
 * Horizontal Bar Chart composable with touch selection and 2D Zoom & Pan gestures.
 */
@Composable
public fun KoraHorizontalBarChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onBarSelected: ((dataset: Dataset?) -> Unit)? = null,
) {
    val renderer = remember { HorizontalBarRenderer() }
    val progress = remember { Animatable(0f) }

    var selectedDataset by remember { mutableStateOf<Dataset?>(null) }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
    }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig = remember(config, selectedDataset, zoomScaleX, panOffsetX) {
        val selectedEntry = selectedDataset?.entries?.firstOrNull()
        config.copy(
            selectedEntry = selectedEntry,
            zoomScaleX = zoomScaleX,
            panOffsetX = panOffsetX,
        )
    }

    val semanticsSummary = remember(datasets) {
        val entries = datasets.flatMap { it.entries }
        "Horizontal bar chart with ${entries.size} bars."
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(datasets, config, zoomScaleX, panOffsetX, selectedDataset) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val topPadding = config.paddingDp * density
                        val bottomPadding = config.paddingDp * density
                        val drawHeight = size.height.toFloat() - topPadding - bottomPadding

                        val count = datasets.size
                        if (count > 0 && drawHeight > 0f) {
                            val slotHeight = drawHeight / count
                            val relativeY = offset.y - topPadding
                            val clickedIdx = (relativeY / slotHeight).toInt()

                            if (clickedIdx in 0 until count) {
                                val tapped = datasets[clickedIdx]
                                if (selectedDataset == tapped) {
                                    selectedDataset = null
                                    onBarSelected?.invoke(null)
                                } else {
                                    selectedDataset = tapped
                                    onBarSelected?.invoke(tapped)
                                }
                            } else {
                                selectedDataset = null
                                onBarSelected?.invoke(null)
                            }
                        }
                    }
                }
                .pointerInput(datasets, config) {
                    if (config.enableZoom) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScaleX = (zoomScaleX * zoom).coerceIn(1f, 5f)
                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding = config.paddingDp * density + 28f * density
                            val rightPadding = config.paddingDp * density + 10f * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val maxPanPixels =
                                (drawWidth * zoomScaleX - drawWidth).coerceAtLeast(0f)
                            panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPanPixels)
                        }
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    data = datasets,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
        }
    }
}

/**
 * Stacked Bar Chart composable supporting touch selection, labels, accessibility, and 2D Zoom & Pan gestures.
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

    val semanticsSummary = remember(entries) {
        "Stacked bar chart with ${entries.size} columns."
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
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
                                    selectedBar = null
                                    onBarSelected?.invoke(null)
                                } else {
                                    selectedBar = tapped
                                    onBarSelected?.invoke(tapped)
                                }
                            } else {
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
                            val rightPadding = config.paddingDp * density + 10f * density
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
                            val rightPadding = config.paddingDp * density + 10f * density
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
                            val rightPadding = config.paddingDp * density + 10f * density
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
 * Financial Candlestick Chart composable with touch selection and 2D Zoom & Pan gestures.
 */
@Composable
public fun KoraCandlestickChart(
    candles: List<CandlestickEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onCandleSelected: ((candle: CandlestickEntry?) -> Unit)? = null,
) {
    val renderer = remember { CandlestickRenderer() }
    val progress = remember { Animatable(0f) }

    var selectedCandle by remember { mutableStateOf<CandlestickEntry?>(null) }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }
    var zoomScaleY by remember { mutableFloatStateOf(config.zoomScaleY) }
    var panOffsetY by remember { mutableFloatStateOf(config.panOffsetY) }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX, config.zoomScaleY, config.panOffsetY) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
        zoomScaleY = config.zoomScaleY
        panOffsetY = config.panOffsetY
    }

    LaunchedEffect(candles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig =
        remember(config, selectedCandle, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
            val selectedEntry = selectedCandle?.let { Entry(it.x, it.close, it.label) }
            config.copy(
                selectedEntry = selectedEntry,
                zoomScaleX = zoomScaleX,
                panOffsetX = panOffsetX,
                zoomScaleY = zoomScaleY,
                panOffsetY = panOffsetY,
            )
        }

    val semanticsSummary = remember(candles) {
        "Financial candlestick chart with ${candles.size} candles."
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(candles, config, zoomScaleX, panOffsetX, selectedCandle) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val leftPadding =
                            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                        val rightPadding = config.paddingDp * density + 10f * density
                        val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                        val count = candles.size
                        if (count > 0 && drawWidth > 0f) {
                            val effectiveZoomX = maxOf(1f, zoomScaleX)
                            val scaledSlotWidth = (drawWidth * effectiveZoomX) / count
                            val relativeX = offset.x - leftPadding + panOffsetX
                            val clickedIdx = (relativeX / scaledSlotWidth).toInt()

                            if (clickedIdx in 0 until count) {
                                val tapped = candles[clickedIdx]
                                if (selectedCandle == tapped) {
                                    selectedCandle = null
                                    onCandleSelected?.invoke(null)
                                } else {
                                    selectedCandle = tapped
                                    onCandleSelected?.invoke(tapped)
                                }
                            } else {
                                selectedCandle = null
                                onCandleSelected?.invoke(null)
                            }
                        }
                    }
                }
                .pointerInput(candles, config) {
                    if (config.enableZoom) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScaleX = (zoomScaleX * zoom).coerceIn(1f, 5f)
                            zoomScaleY = (zoomScaleY * zoom).coerceIn(1f, 5f)

                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding = config.paddingDp * density + 10f * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val maxPanPixels =
                                (drawWidth * zoomScaleX - drawWidth).coerceAtLeast(0f)
                            panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPanPixels)

                            val minVal = candles.minOfOrNull { it.low } ?: 0f
                            val maxVal = candles.maxOfOrNull { it.high } ?: 100f
                            val rangeY = (maxVal - minVal).coerceAtLeast(1f)
                            val visibleRangeY = rangeY / zoomScaleY
                            val maxPanY = (rangeY - visibleRangeY).coerceAtLeast(0f)
                            val deltaDataY = pan.y / size.height.toFloat() * visibleRangeY
                            panOffsetY = (panOffsetY + deltaDataY).coerceIn(0f, maxPanY)
                        }
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    data = candles,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
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

    val semanticsSummary = remember(datasets) {
        "Radar chart with ${datasets.size} polygons."
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = semanticsSummary
            role = Role.Image
        }
    ) {
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
 * Combined Bar + Line Overlay Chart composable with touch selection.
 */
@Composable
public fun KoraCombinedChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onPointSelected: ((x: Float, y: Float) -> Unit)? = null,
) {
    val renderer = remember { CombinedRenderer() }
    val progress = remember { Animatable(0f) }

    var selectedPoint by remember { mutableStateOf(config.selectedEntry) }

    LaunchedEffect(config.selectedEntry) {
        selectedPoint = config.selectedEntry
    }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig = remember(config, selectedPoint) {
        config.copy(selectedEntry = selectedPoint)
    }

    val semanticsSummary = remember(datasets) {
        "Combined bar and line chart with ${datasets.size} series."
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(datasets, config, selectedPoint) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val engine = ChartEngine(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            config.paddingDp * density
                        )
                        val nearest = engine.nearestLineEntry(
                            offset.x,
                            offset.y,
                            datasets,
                            activeConfig,
                            density
                        )

                        if (nearest != null) {
                            if (selectedPoint == nearest) {
                                selectedPoint = null
                            } else {
                                selectedPoint = nearest
                                onPointSelected?.invoke(nearest.x, nearest.y)
                            }
                        } else {
                            selectedPoint = null
                        }
                    }
                }
                .pointerInput(datasets, config) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val density = Resources.getSystem().displayMetrics.density
                        val engine = ChartEngine(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            config.paddingDp * density
                        )
                        val nearest = engine.nearestLineEntry(
                            change.position.x,
                            change.position.y,
                            datasets,
                            activeConfig,
                            density
                        )
                        if (nearest != null && nearest != selectedPoint) {
                            selectedPoint = nearest
                            onPointSelected?.invoke(nearest.x, nearest.y)
                        }
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    data = datasets,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
        }
    }
}

/**
 * Bubble Scatter Plot Chart composable with 2D Euclidean touch selection and Zoom & Pan gestures.
 */
@Composable
public fun KoraBubbleChart(
    bubbles: List<BubbleEntry>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onBubbleSelected: ((bubble: BubbleEntry?) -> Unit)? = null,
) {
    val renderer = remember { BubbleRenderer() }
    val progress = remember { Animatable(0f) }

    var selectedBubble by remember { mutableStateOf<BubbleEntry?>(null) }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }
    var zoomScaleY by remember { mutableFloatStateOf(config.zoomScaleY) }
    var panOffsetY by remember { mutableFloatStateOf(config.panOffsetY) }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX, config.zoomScaleY, config.panOffsetY) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
        zoomScaleY = config.zoomScaleY
        panOffsetY = config.panOffsetY
    }

    LaunchedEffect(bubbles) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig =
        remember(config, selectedBubble, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
            val selectedEntry = selectedBubble?.let { Entry(it.x, it.y, it.label) }
            config.copy(
                selectedEntry = selectedEntry,
                zoomScaleX = zoomScaleX,
                panOffsetX = panOffsetX,
                zoomScaleY = zoomScaleY,
                panOffsetY = panOffsetY,
            )
        }

    val semanticsSummary = remember(bubbles) {
        "Bubble scatter plot with ${bubbles.size} items."
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(
                    bubbles,
                    config,
                    zoomScaleX,
                    panOffsetX,
                    zoomScaleY,
                    panOffsetY,
                    selectedBubble
                ) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val leftPadding =
                            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                        val rightPadding = config.paddingDp * density + 18f * density
                        val bottomPadding =
                            if (config.showAxisLabels) config.paddingDp * density + 20f * density else config.paddingDp * density
                        val topPadding = config.paddingDp * density + 10f * density
                        val topHeadroom = 28f * density

                        val drawWidth = size.width.toFloat() - leftPadding - rightPadding
                        val drawHeight =
                            size.height.toFloat() - topPadding - bottomPadding - topHeadroom

                        val xHeadroom = 28f * density
                        val yHeadroom = 28f * density

                        val usableDrawWidth = (drawWidth - xHeadroom).coerceAtLeast(1f)
                        val usableDrawHeight = (drawHeight - yHeadroom).coerceAtLeast(1f)

                        val maxX = bubbles.maxOfOrNull { it.x }?.coerceAtLeast(1f) ?: 1f
                        val maxY = bubbles.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
                        val baselineY = topPadding + drawHeight

                        val count = bubbles.size
                        if (count > 0 && drawWidth > 0f) {
                            var nearest: BubbleEntry? = null
                            var minDist = Float.MAX_VALUE

                            val visibleZoomY = maxOf(1f, zoomScaleY)
                            val visibleRangeY =
                                if (visibleZoomY == 1f) maxY else maxY / visibleZoomY
                            val clampedPanY =
                                panOffsetY.coerceIn(0f, (maxY - visibleRangeY).coerceAtLeast(0f))
                            val effectiveMinY = clampedPanY

                            for (bubble in bubbles) {
                                val pctX = bubble.x / maxX
                                val cx = leftPadding + (pctX * usableDrawWidth * maxOf(
                                    1f,
                                    zoomScaleX
                                )) - panOffsetX
                                val pctY = (bubble.y - effectiveMinY) / visibleRangeY
                                val cy = baselineY - yHeadroom - (pctY * usableDrawHeight)

                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val threshold = maxOf(bubble.radiusDp * density, 24f * density)

                                if (dist < minDist && dist <= threshold) {
                                    minDist = dist
                                    nearest = bubble
                                }
                            }

                            if (nearest != null) {
                                if (selectedBubble == nearest) {
                                    selectedBubble = null
                                    onBubbleSelected?.invoke(null)
                                } else {
                                    selectedBubble = nearest
                                    onBubbleSelected?.invoke(nearest)
                                }
                            } else {
                                selectedBubble = null
                                onBubbleSelected?.invoke(null)
                            }
                        }
                    }
                }
                .pointerInput(bubbles, config) {
                    if (config.enableZoom) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScaleX = (zoomScaleX * zoom).coerceIn(1f, 5f)
                            zoomScaleY = (zoomScaleY * zoom).coerceIn(1f, 5f)

                            val density = Resources.getSystem().displayMetrics.density
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding = config.paddingDp * density + 18f * density
                            val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                            val maxPanPixels =
                                (drawWidth * zoomScaleX - drawWidth).coerceAtLeast(0f)
                            panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPanPixels)

                            val maxY = bubbles.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
                            val visibleRangeY = maxY / zoomScaleY
                            val maxPanY = (maxY - visibleRangeY).coerceAtLeast(0f)
                            val deltaDataY = pan.y / size.height.toFloat() * visibleRangeY
                            panOffsetY = (panOffsetY + deltaDataY).coerceIn(0f, maxPanY)
                        }
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    data = bubbles,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
        }
    }
}
