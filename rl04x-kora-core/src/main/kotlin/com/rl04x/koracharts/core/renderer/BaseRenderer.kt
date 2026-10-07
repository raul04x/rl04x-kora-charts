package com.rl04x.koracharts.core.renderer

import android.graphics.Canvas
import com.rl04x.koracharts.core.model.ChartConfig

/**
 * Base contract for all Kora chart renderers.
 *
 * Each chart type (Line, Bar, Pie, etc.) implements this interface to draw onto an Android [Canvas].
 *
 * @param T Specific dataset model type
 */
public interface BaseRenderer<T> {

    /**
     * Draws the chart onto the given [Canvas] within the specified width and height bounds.
     *
     * @param canvas Target Android canvas
     * @param width Drawing area width in px
     * @param height Drawing area height in px
     * @param data List of dataset models to render
     * @param config Chart visual configuration
     * @param progress Animation progress ∈ [0f, 1f] (1f = complete)
     */
    public fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        data: List<T>,
        config: ChartConfig,
        progress: Float = 1f,
    )
}
