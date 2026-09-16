package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.axelliant.hris.R

class SignaturePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val signaturePath = Path()
    private val signaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.ds_text_primary)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = resources.displayMetrics.density * 2.2f
        style = Paint.Style.STROKE
    }
    private var hasSignature = false
    var onSignatureChanged: ((Boolean) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawPath(signaturePath, signaturePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                signaturePath.moveTo(event.x, event.y)
                updateSignatureState(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                signaturePath.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }

    fun clear() {
        signaturePath.reset()
        updateSignatureState(false)
        invalidate()
    }

    private fun updateSignatureState(value: Boolean) {
        if (hasSignature == value) return
        hasSignature = value
        onSignatureChanged?.invoke(value)
    }
}
