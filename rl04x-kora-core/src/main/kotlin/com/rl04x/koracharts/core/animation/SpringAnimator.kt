package com.rl04x.koracharts.core.animation

import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator

/**
 * Spring animator with an overshoot physical spring effect.
 */
public class SpringAnimator(
    private val tension: Float = 1.2f,
) : KoraAnimator {

    private var animator: ValueAnimator? = null

    override val isRunning: Boolean
        get() = animator?.isRunning == true

    override fun start(durationMs: Long, onUpdate: (Float) -> Unit) {
        cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = OvershootInterpolator(tension)
            addUpdateListener { onUpdate(it.animatedValue as Float) }
            start()
        }
    }

    override fun cancel() {
        animator?.cancel()
        animator = null
    }
}
