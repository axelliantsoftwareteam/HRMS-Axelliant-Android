package com.axelliant.hris.extention

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isVisible
import com.facebook.shimmer.ShimmerFrameLayout

private const val MIN_SHIMMER_DURATION_MS = 1000L

fun ShimmerFrameLayout.showShimmer(contentView: View) {
    tag = System.currentTimeMillis()
    startShimmer()
    isVisible = true
    contentView.isVisible = false
}

fun ShimmerFrameLayout.hideShimmer(contentView: View) {
    val startTime = tag as? Long ?: 0L
    val elapsed = System.currentTimeMillis() - startTime
    val remaining = MIN_SHIMMER_DURATION_MS - elapsed

    val hide = {
        stopShimmer()
        isVisible = false
        contentView.isVisible = true
    }

    if (remaining > 0) {
        Handler(Looper.getMainLooper()).postDelayed(hide, remaining)
    } else {
        hide()
    }
}