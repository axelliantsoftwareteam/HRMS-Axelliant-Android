package com.axelliant.hris.core.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

class ShimmerAnimatorHelper {
    private val animators = mutableListOf<ObjectAnimator>()

    fun populate(
        container: ViewGroup,
        inflater: LayoutInflater,
        @LayoutRes itemLayoutRes: Int,
        count: Int
    ) {
        clear(container)
        repeat(count) { index ->
            val view = inflater.inflate(itemLayoutRes, container, false)
            container.addView(view)
            startAnimation(view, index)
        }
    }

    fun clear(container: ViewGroup) {
        release()
        container.removeAllViews()
    }

    fun release() {
        animators.forEach { it.cancel() }
        animators.clear()
    }

    private fun startAnimation(view: View, index: Int) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, SHIMMER_MIN_ALPHA, 1f).apply {
            duration = SHIMMER_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = (index % SHIMMER_STAGGER_MODULO) * SHIMMER_STAGGER_MS
            start()
        }
        animators.add(animator)
    }

    companion object {
        private const val SHIMMER_MIN_ALPHA = 0.45f
        private const val SHIMMER_DURATION_MS = 700L
        private const val SHIMMER_STAGGER_MS = 80L
        private const val SHIMMER_STAGGER_MODULO = 6
    }
}
