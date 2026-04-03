package com.myaunt.app.ui

import android.view.View
import android.view.animation.DecelerateInterpolator

object UiMotion {

    fun staggerFadeUp(
        views: List<View>,
        liftPx: Float,
        itemDelayMs: Long = 48,
        durationMs: Long = 380,
    ) {
        views.forEach { v ->
            v.animate().cancel()
            v.alpha = 0f
            v.translationY = liftPx
        }
        views.forEachIndexed { i, v ->
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(durationMs)
                .setStartDelay(i * itemDelayMs)
                .setInterpolator(DecelerateInterpolator(1.2f))
                .start()
        }
    }
}
