package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.axelliant.hris.R

/**
 * XML bridge for checkbox controls.
 * Fluent2 does not provide a stable drop-in XML checkbox view here, so this keeps XML layouts
 * safe while applying the shared Fluent2 design tokens.
 */
class AppCheckboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.checkboxStyle,
) : AppCompatCheckBox(context, attrs, defStyleAttr) {
    init {
        layoutDirection = LAYOUT_DIRECTION_LTR
        textDirection = TEXT_DIRECTION_LTR
        includeFontPadding = false
        if (!hasExplicitTextColor(attrs)) {
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary))
        }
        if (buttonTintList == null) {
            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf()
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.ds_primary),
                    ContextCompat.getColor(context, R.color.ds_outline_strong),
                    ContextCompat.getColor(context, R.color.ds_outline_strong)
                )
            )
        }
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(LAYOUT_DIRECTION_LTR)
        this.layoutDirection = LAYOUT_DIRECTION_LTR
        textDirection = TEXT_DIRECTION_LTR
    }

    private fun hasExplicitTextColor(attrs: AttributeSet?): Boolean {
        if (attrs == null) return false
        return context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textColor)).use {
            it.hasValue(0)
        }
    }
}
