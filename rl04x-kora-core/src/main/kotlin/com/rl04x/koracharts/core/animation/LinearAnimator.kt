package com.rl04x.koracharts.core.animation

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

/**
 * Linear animator with constant velocity interpolation.
 */
public class LinearAnimator : KoraAnimator {

    private var animator: ValueAnimator? = null

    override val isRunning: Boolean
        get() = animator?.isRunning == true

    override fun start(durationMs: Long, onUpdate: (Float) -> Unit) {
        cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration     = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { onUpdate(it.animatedValue as Float) }
            start()
        }
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }
}
