package com.axelliant.hris.ui.designsystem.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.axelliant.hris.R

/**
 * XML bridge for progress indicators.
 * The platform ProgressBar remains the safest XML control while Fluent2 color tokens drive it.
 */
class AppProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyle,
) : ProgressBar(context, attrs, defStyleAttr) {
    init {
        applyXmlTintAttributes(attrs)
        applyDefaultIndicatorColor()
    }

    fun setIndicatorColor(color: Int) {
        progressTintList = ColorStateList.valueOf(color)
        secondaryProgressTintList = ColorStateList.valueOf(color)
        indeterminateTintList = ColorStateList.valueOf(color)
    }

    private fun applyDefaultIndicatorColor() {
        val color = ContextCompat.getColor(context, R.color.ds_primary)
        if (progressTintList == null) {
            progressTintList = ColorStateList.valueOf(color)
        }
        if (secondaryProgressTintList == null) {
            secondaryProgressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.ds_primary_container)
            )
        }
        if (indeterminateTintList == null) {
            indeterminateTintList = ColorStateList.valueOf(color)
        }
    }

    @SuppressLint("ResourceType")
    private fun applyXmlTintAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        context.obtainStyledAttributes(
            attrs,
            intArrayOf(
                android.R.attr.progressTint,
                android.R.attr.secondaryProgressTint,
                android.R.attr.indeterminateTint,
            )
        ).use {
            it.getColorStateList(ANDROID_ATTR_PROGRESS_TINT)
                ?.let { tint -> progressTintList = tint }
            it.getColorStateList(ANDROID_ATTR_SECONDARY_PROGRESS_TINT)
                ?.let { tint -> secondaryProgressTintList = tint }
            it.getColorStateList(ANDROID_ATTR_INDETERMINATE_TINT)
                ?.let { tint -> indeterminateTintList = tint }
        }
    }

    private companion object {
        const val ANDROID_ATTR_PROGRESS_TINT = 0
        const val ANDROID_ATTR_SECONDARY_PROGRESS_TINT = 1
        const val ANDROID_ATTR_INDETERMINATE_TINT = 2
    }
}
