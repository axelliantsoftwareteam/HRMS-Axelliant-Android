package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.drawable.DrawableCompat
import com.axelliant.hris.R

class AppTextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {
    private var iconTint: ColorStateList? = null
    private var iconPadding: Int = 0
        set(value) {
            field = value
            compoundDrawablePadding = value
        }
    private var iconGravity: Int = ICON_GRAVITY_TEXT_START
    private var icon: Drawable? = null

    init {
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        minHeight = resources.getDimensionPixelSize(R.dimen.ds_input_height)
        setSingleLine(true)
        context.obtainStyledAttributes(attrs, R.styleable.AppTextFieldView, defStyleAttr, 0).use {
            iconPadding = it.getDimensionPixelSize(R.styleable.AppTextFieldView_iconPadding, iconPadding)
            iconTint = it.getColorStateList(R.styleable.AppTextFieldView_iconTint)
            iconGravity = it.getInt(R.styleable.AppTextFieldView_iconGravity, iconGravity)
            icon = it.getDrawable(R.styleable.AppTextFieldView_icon)
        }
        applyRawXmlIconAttributes(attrs)
        applyIcon()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(View.LAYOUT_DIRECTION_LTR)
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        applyIcon()
    }

    private fun applyIcon() {
        val drawable = icon?.mutate()?.let { source ->
            DrawableCompat.wrap(source).also { wrapped ->
                iconTint?.let { DrawableCompat.setTintList(wrapped, it) }
                if (wrapped.bounds.isEmpty) {
                    wrapped.setBounds(0, 0, wrapped.intrinsicWidth, wrapped.intrinsicHeight)
                }
            }
        }

        if (iconGravity == ICON_GRAVITY_TEXT_END) {
            setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }
    }

    private fun applyRawXmlIconAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val rawIconResId = attrs.getAttributeResourceValue(AUTO_NS, "icon", 0)
        if (icon == null && rawIconResId != 0) {
            icon = AppCompatResources.getDrawable(context, rawIconResId)
        }

        val rawTintResId = attrs.getAttributeResourceValue(AUTO_NS, "iconTint", 0)
        if (iconTint == null && rawTintResId != 0) {
            iconTint = ContextCompat.getColorStateList(context, rawTintResId)
        }

        iconPadding = attrs.getAttributeResourceValue(AUTO_NS, "iconPadding", 0)
            .takeIf { it != 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: iconPadding

        when (attrs.getAttributeValue(AUTO_NS, "iconGravity")) {
            "start" -> iconGravity = ICON_GRAVITY_TEXT_START
            "end" -> iconGravity = ICON_GRAVITY_TEXT_END
        }
    }

    private companion object {
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val ICON_GRAVITY_TEXT_START = 1
        const val ICON_GRAVITY_TEXT_END = 2
    }
}
