package com.axelliant.hris.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.LinearLayout
import android.view.View
import android.view.animation.LinearInterpolator

/** Lightweight placeholder sweep, with no third-party dependency and static reduced-motion fallback. */
class WsShimmerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val paint = Paint()
    private val matrix = Matrix()
    private var animator: ValueAnimator? = null
    private var requested = false
    private var offset = 0f

    fun setAnimating(enabled: Boolean) {
        requested = enabled
        if (!enabled) stopAnimation() else startAnimation()
    }

    private fun startAnimation() {
        if (!requested || !isAttachedToWindow || !isShown || animator != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()) return
        animator = ValueAnimator.ofFloat(-1f, 2f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { offset = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) paint.shader = LinearGradient(0f, 0f, w * .55f, 0f,
            intArrayOf(Color.TRANSPARENT, 0x66FFFFFF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (animator != null && paint.shader != null) {
            matrix.setTranslate(offset * width, 0f)
            paint.shader.setLocalMatrix(matrix)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startAnimation() }
    override fun onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isShown) startAnimation() else stopAnimation()
    }
}
