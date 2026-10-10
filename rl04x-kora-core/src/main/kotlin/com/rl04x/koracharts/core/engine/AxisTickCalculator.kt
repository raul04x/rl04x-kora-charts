package com.rl04x.koracharts.core.engine

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * Result model representing calculated nice ticks for chart axes.
 */
public data class TickResult(
    val ticks: List<Float>,
    val step: Float,
    val niceMin: Float,
    val niceMax: Float,
)

/**
 * Mathematical calculator for generating "nice" axis tick marks and intervals.
 * Produces clean whole-number intervals like 1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, etc.
 */
public object AxisTickCalculator {

    /**
     * Computes a "nice" step size for a given numeric [range].
     *s
     * @param range The span (max - min) of values.
     * @param targetTicks Target number of tick divisions (default 4).
     * @param forceInteger If true, guarantees step is at least 1.0 and a whole number.
     */
    public fun computeNiceStep(
        range: Float,
        targetTicks: Int = 4,
        forceInteger: Boolean = true,
    ): Float {
        val safeRange = if (range <= 0f) 1f else range
        val rawStep = safeRange / targetTicks.coerceAtLeast(1)

        val exponent = floor(log10(rawStep.toDouble())).toFloat()
        val fraction = rawStep / 10f.pow(exponent)

        val niceFraction = when {
            fraction < 1.5f -> 1f
            fraction < 3f -> 2f
            fraction < 7f -> 5f
            else -> 10f
        }

        var niceStep = niceFraction * 10f.pow(exponent)

        if (forceInteger) {
            niceStep = maxOf(1f, round(niceStep))
        }

        return niceStep
    }

    /**
     * Calculates nice tick values and bounded range for [min] and [max].
     *
     * @param min Minimum data value.
     * @param max Maximum data value.
     * @param targetTicks Target number of tick divisions (default 4).
     * @param customStep Optional manual override step size (e.g., 5f, 100f).
     * @param forceInteger If true, guarantees step and ticks are rounded whole numbers.
     * @param capAtMax If true, caps the upper axis bound and final tick at [max] to eliminate empty canvas space.
     */
    public fun computeNiceTicks(
        min: Float,
        max: Float,
        targetTicks: Int = 4,
        customStep: Float? = null,
        forceInteger: Boolean = true,
        capAtMax: Boolean = true
    ): TickResult {
        var effectiveMax = max
        if (min >= effectiveMax) {
            effectiveMax = min + 1f
        }

        val range = effectiveMax - min
        val step = if (customStep != null && customStep > 0f) {
            if (forceInteger) maxOf(1f, round(customStep)) else customStep
        } else {
            computeNiceStep(range, targetTicks, forceInteger)
        }

        val niceMin = floor(min / step) * step
        var niceMax = ceil(effectiveMax / step) * step

        if (capAtMax && niceMax > effectiveMax) {
            niceMax = effectiveMax
        }

        val ticks = mutableListOf<Float>()
        var current = niceMin
        val epsilon = step * 0.0001f
        while (current <= niceMax - epsilon) {
            val formatted = if (forceInteger) round(current) else current
            ticks.add(formatted)
            current += step
        }

        val formattedMax = if (forceInteger) round(niceMax) else niceMax
        val minTickGap = step * 0.45f

        if (ticks.isEmpty()) {
            ticks.add(formattedMax)
        } else {
            val lastTick = ticks.last()
            if (formattedMax > lastTick + epsilon) {
                if (formattedMax - lastTick < minTickGap && ticks.size > 1) {
                    ticks[ticks.lastIndex] = formattedMax
                } else {
                    ticks.add(formattedMax)
                }
            }
        }

        return TickResult(
            ticks = ticks,
            step = step,
            niceMin = niceMin,
            niceMax = niceMax,
        )
    }
}
