package com.axelliant.hris.core.extensions

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.axelliant.hris.R

fun EditText.enableClearTextButton(
    @DrawableRes iconRes: Int = R.drawable.ia_ic_filter_close
) {
    val clearDrawable = ContextCompat.getDrawable(context, iconRes)?.mutate()?.let { drawable ->
        DrawableCompat.wrap(drawable).also {
            DrawableCompat.setTint(it, ContextCompat.getColor(context, R.color.ds_text_muted))
            val size = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._14sdp)
            it.setBounds(0, 0, size, size)
        }
    } ?: return

    fun updateClearIcon() {
        val drawables = compoundDrawablesRelative
        setCompoundDrawablesRelative(
            drawables[0],
            drawables[1],
            clearDrawable.takeIf { text?.isNotEmpty() == true },
            drawables[3]
        )
        compoundDrawablePadding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
    }

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(text: Editable?) {
            updateClearIcon()
        }
    })

    setOnTouchListener { view, event ->
        val endDrawable = compoundDrawablesRelative[2] ?: return@setOnTouchListener false
        if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

        val touchStart = width - paddingEnd - endDrawable.bounds.width() -
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        if (event.x >= touchStart) {
            setText("")
            view.performClick()
            true
        } else {
            false
        }
    }

    updateClearIcon()
}
