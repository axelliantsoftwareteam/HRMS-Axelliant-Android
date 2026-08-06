package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ProgressBar

class AppProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyle,
) : ProgressBar(context, attrs, defStyleAttr) {
    fun setIndicatorColor(color: Int) {
        progressTintList = ColorStateList.valueOf(color)
        indeterminateTintList = ColorStateList.valueOf(color)
    }
}
