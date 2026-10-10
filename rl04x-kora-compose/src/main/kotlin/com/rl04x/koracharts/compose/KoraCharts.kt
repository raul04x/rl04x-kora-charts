package com.rl04x.koracharts.compose

import android.content.res.Resources
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rl04x.koracharts.core.engine.ChartEngine
import com.rl04x.koracharts.core.model.ChartConfig
import com.rl04x.koracharts.core.model.Dataset
import com.rl04x.koracharts.core.model.Entry
import com.rl04x.koracharts.core.renderer.BarRenderer
import com.rl04x.koracharts.core.renderer.LineRenderer
import com.rl04x.koracharts.core.renderer.PieRenderer

/**
 * Kora Line Chart composable supporting touch selection, 2D Zoom & Pan gestures, accessibility, and glassmorphic badge overlays.
 */
@Composable
public fun KoraLineChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onPointSelected: ((x: Float, y: Float) -> Unit)? = null,
) {
    val renderer = remember { LineRenderer() }
    val progress = remember { Animatable(0f) }
    var selectedPoint by remember { mutableStateOf<Entry?>(config.selectedEntry) }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }
    var zoomScaleY by remember { mutableFloatStateOf(config.zoomScaleY) }
    var panOffsetY by remember { mutableFloatStateOf(config.panOffsetY) }

    LaunchedEffect(config.selectedEntry) {
        selectedPoint = config.selectedEntry
    }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX, config.zoomScaleY, config.panOffsetY) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
        zoomScaleY = config.zoomScaleY
        panOffsetY = config.panOffsetY
    }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig =
        remember(config, selectedPoint, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
            config.copy(
                selectedEntry = selectedPoint,
                zoomScaleX = zoomScaleX,
                panOffsetX = panOffsetX,
                zoomScaleY = zoomScaleY,
                panOffsetY = panOffsetY,
            )
        }

    val semanticsSummary = remember(datasets) {
        val allEntries = datasets.flatMap { it.entries }
        if (allEntries.isEmpty()) {
            "Line chart with no data"
        } else {
            val minVal = allEntries.minOf { it.y }
            val maxVal = allEntries.maxOf { it.y }
            "Line chart with ${allEntries.size} points. Minimum $minVal, Maximum $maxVal."
        }
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(datasets, config, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val density = Resources.getSystem().displayMetrics.density
                            val engine =
                                ChartEngine(
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    config.paddingDp * density
                                )
                            val nearest = engine.nearestLineEntry(
                                offset.x, offset.y, datasets, activeConfig, density,
                            )
                            selectedPoint = nearest
                            nearest?.let { onPointSelected?.invoke(it.x, it.y) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val density = Resources.getSystem().displayMetrics.density
                            val engine =
                                ChartEngine(
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    config.paddingDp * density
                                )
                            val nearest = engine.nearestLineEntry(
                                change.position.x,
                                change.position.y,
                                datasets,
                                activeConfig,
                                density,
                            )
                            if (nearest != selectedPoint) {
                                selectedPoint = nearest
                                nearest?.let { onPointSelected?.invoke(it.x, it.y) }
                            }
                        }
                    )
                }
                .pointerInput(
                    datasets,
                    config,
                    zoomScaleX,
                    panOffsetX,
                    zoomScaleY,
                    panOffsetY,
                    selectedPoint
                ) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val engine =
                            ChartEngine(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                config.paddingDp * density
                            )
                        val nearest = engine.nearestLineEntry(
                            offset.x, offset.y, datasets, activeConfig, density,
                        )
                        if (selectedPoint != null && selectedPoint == nearest) {
                            selectedPoint = null
                        } else {
                            selectedPoint = nearest
                            nearest?.let { onPointSelected?.invoke(it.x, it.y) }
                        }
                    }
                }
                .pointerInput(datasets, config) {
                    if (config.enableZoom) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScaleX = (zoomScaleX * zoom).coerceIn(1f, 5f)
                            zoomScaleY = (zoomScaleY * zoom).coerceIn(1f, 5f)

                            val density = Resources.getSystem().displayMetrics.density
                            val hasSecondaryY =
                                config.showSecondaryYAxis || datasets.any { it.useSecondaryAxis }
                            val leftPadding =
                                if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                            val rightPadding =
                                if (hasSecondaryY && config.showAxisLabels) config.paddingDp * density + 32f * density else config.paddingDp * density + 10f * density
                            val drawWidth =
                                (size.width.toFloat() - leftPadding - rightPadding).coerceAtLeast(1f)

                            val maxPanXPixels =
                                (drawWidth * zoomScaleX - drawWidth).coerceAtLeast(0f)
                            panOffsetX = (panOffsetX - pan.x).coerceIn(0f, maxPanXPixels)

                            val engine = ChartEngine(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                config.paddingDp * density
                            )
                            val range = engine.computeRange(datasets)
                            val visibleRangeY = range.rangeY / zoomScaleY
                            val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
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
                    data = datasets,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
        }
    }
}

/**
 * Kora Bar Chart composable supporting touch selection, accessibility, and 2D Zoom & Pan gestures.
 */
@Composable
public fun KoraBarChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    onBarSelected: ((x: Float, y: Float) -> Unit)? = null,
) {
    val renderer = remember { BarRenderer() }
    val progress = remember { Animatable(0f) }
    var selectedBar by remember { mutableStateOf<Entry?>(config.selectedEntry) }
    var zoomScaleX by remember { mutableFloatStateOf(config.zoomScaleX) }
    var panOffsetX by remember { mutableFloatStateOf(config.panOffsetX) }
    var zoomScaleY by remember { mutableFloatStateOf(config.zoomScaleY) }
    var panOffsetY by remember { mutableFloatStateOf(config.panOffsetY) }

    LaunchedEffect(config.selectedEntry) {
        selectedBar = config.selectedEntry
    }

    LaunchedEffect(config.zoomScaleX, config.panOffsetX, config.zoomScaleY, config.panOffsetY) {
        zoomScaleX = config.zoomScaleX
        panOffsetX = config.panOffsetX
        zoomScaleY = config.zoomScaleY
        panOffsetY = config.panOffsetY
    }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig =
        remember(config, selectedBar, zoomScaleX, panOffsetX, zoomScaleY, panOffsetY) {
            config.copy(
                selectedEntry = selectedBar,
                zoomScaleX = zoomScaleX,
                panOffsetX = panOffsetX,
                zoomScaleY = zoomScaleY,
                panOffsetY = panOffsetY,
            )
        }

    val semanticsSummary = remember(datasets) {
        val allEntries = datasets.flatMap { it.entries }
        if (allEntries.isEmpty()) {
            "Bar chart with no data"
        } else {
            val minVal = allEntries.minOf { it.y }
            val maxVal = allEntries.maxOf { it.y }
            "Bar chart with ${allEntries.size} items. Minimum $minVal, Maximum $maxVal."
        }
    }

    Box(modifier = modifier.semantics {
        contentDescription = semanticsSummary
        role = Role.Image
    }) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(datasets, config, zoomScaleX, panOffsetX, selectedBar) {
                    detectTapGestures { offset ->
                        val density = Resources.getSystem().displayMetrics.density
                        val leftPadding =
                            if (config.showAxisLabels) config.paddingDp * density + 28f * density else config.paddingDp * density
                        val rightPadding = config.paddingDp * density
                        val drawWidth = size.width.toFloat() - leftPadding - rightPadding

                        val visibleDatasets =
                            datasets.filter { it.visible && it.entries.isNotEmpty() }
                        val entryCount = visibleDatasets.firstOrNull()?.entries?.size ?: 0
                        if (entryCount > 0 && drawWidth > 0f) {
                            val effectiveZoomX = maxOf(1f, zoomScaleX)
                            val scaledSlotWidth = (drawWidth * effectiveZoomX) / entryCount
                            val relativeX = offset.x - leftPadding + panOffsetX
                            val clickedIdx = (relativeX / scaledSlotWidth).toInt()

                            if (clickedIdx in 0 until entryCount) {
                                val tapped = visibleDatasets.first().entries.getOrNull(clickedIdx)
                                if (selectedBar != null && selectedBar == tapped) {
                                    selectedBar = null
                                } else {
                                    selectedBar = tapped
                                    tapped?.let { onBarSelected?.invoke(it.x, it.y) }
                                }
                            } else {
                                selectedBar = null
                            }
                        }
                    }
                }
                .pointerInput(datasets, config) {
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

                            val engine = ChartEngine(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                config.paddingDp * density
                            )
                            val range = engine.computeRange(datasets)
                            val visibleRangeY = range.rangeY / zoomScaleY
                            val maxPanY = (range.rangeY - visibleRangeY).coerceAtLeast(0f)
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
                    data = datasets,
                    config = activeConfig,
                    progress = progress.value,
                )
            }
        }
    }
}

/**
 * Kora Pie/Donut Chart composable with slice selection, accessibility, and central hole label.
 */
@Composable
public fun KoraPieChart(
    datasets: List<Dataset>,
    modifier: Modifier = Modifier,
    config: ChartConfig = ChartConfig(),
    holeRadius: Float = 0.55f,
    onSliceSelected: ((entry: Entry) -> Unit)? = null,
) {
    val renderer = remember(holeRadius) { PieRenderer(holeRadius) }
    val progress = remember { Animatable(0f) }
    var selectedSlice by remember { mutableStateOf<Entry?>(config.selectedEntry) }

    LaunchedEffect(config.selectedEntry) {
        selectedSlice = config.selectedEntry
    }

    LaunchedEffect(datasets) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = config.animationDuration.toInt()),
        )
    }

    val activeConfig = remember(config, selectedSlice) {
        config.copy(selectedEntry = selectedSlice)
    }

    val semanticsSummary = remember(datasets) {
        val entries = datasets.firstOrNull()?.entries ?: emptyList()
        if (entries.isEmpty()) {
            "Pie chart with no data"
        } else {
            val total = entries.sumOf { it.y.toDouble() }
            "Pie chart with ${entries.size} sectors. Total value: $total."
        }
    }

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = semanticsSummary
                role = Role.Image
            }
            .pointerInput(datasets, config, holeRadius, selectedSlice) {
                detectTapGestures { offset ->
                    val density = Resources.getSystem().displayMetrics.density
                    val padding = config.paddingDp * density
                    val engine = ChartEngine(size.width.toFloat(), size.height.toFloat(), padding)
                    val tappedSlice =
                        engine.findPieEntryAt(offset.x, offset.y, datasets, density)
                    if (selectedSlice != null && selectedSlice == tappedSlice) {
                        selectedSlice = null
                    } else {
                        selectedSlice = tappedSlice
                        tappedSlice?.let { onSliceSelected?.invoke(it) }
                    }
                }
            },
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

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun KoraLineChartPreview() {
    val previewDataset = Dataset(
        entries = listOf(
            Entry(0f, 10f, "Jan"),
            Entry(1f, 25f, "Feb"),
            Entry(2f, 18f, "Mar"),
            Entry(3f, 40f, "Apr"),
            Entry(4f, 32f, "May"),
        ),
        label = "Sales",
        isCurved = true,
        gradientFill = true,
    )
    Column(modifier = Modifier.padding(16.dp)) {
        KoraLineChart(
            datasets = listOf(previewDataset),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KoraBarChartPreview() {
    val previewDataset = Dataset(
        entries = listOf(
            Entry(0f, 15f, "Q1"),
            Entry(1f, 30f, "Q2"),
            Entry(2f, 45f, "Q3"),
            Entry(3f, 25f, "Q4"),
        ),
        label = "Revenue",
    )
    Column(modifier = Modifier.padding(16.dp)) {
        KoraBarChart(
            datasets = listOf(previewDataset),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KoraPieChartPreview() {
    val previewDataset = Dataset(
        entries = listOf(
            Entry(0f, 40f, "Android"),
            Entry(1f, 30f, "iOS"),
            Entry(2f, 20f, "Web"),
            Entry(3f, 10f, "Desktop"),
        ),
    )
    Column(modifier = Modifier.padding(16.dp)) {
        KoraPieChart(
            datasets = listOf(previewDataset),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}
