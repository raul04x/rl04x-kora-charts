package com.rl04x.koracharts.core.animation

/**
 * Animation system contract for Kora Charts.
 *
 * The animator produces Float progress values in [0f, 1f].
 * Renderers consume these values to determine the animation progress of the chart.
 *
 * Available implementations:
 *  - [LinearAnimator] -> constant velocity interpolation
 *  - [SpringAnimator] -> spring overshoot interpolation
 */
public interface KoraAnimator {
    /** Starts the animation. [onUpdate] is invoked on each frame with progress ∈ [0f, 1f]. */
    public fun start(durationMs: Long, onUpdate: (progress: Float) -> Unit)

    /** Cancels the animation if currently running. */
    public fun cancel()

    /** Returns true if the animation is currently running. */
    public val isRunning: Boolean
}
