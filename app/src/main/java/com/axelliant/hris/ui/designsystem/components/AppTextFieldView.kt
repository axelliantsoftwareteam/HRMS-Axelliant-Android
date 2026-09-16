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
        applyDefaultBackground(attrs)
        applyMinHeightToken(attrs)
        applyDefaultTextTokens(attrs)
        if (shouldForceSingleLine(attrs)) {
            setSingleLine(true)
        }
        context.obtainStyledAttributes(attrs, R.styleable.AppTextFieldView, defStyleAttr, 0).use {
            iconPadding = it.getDimensionPixelSize(R.styleable.AppTextFieldView_iconPadding, iconPadding)
            iconTint = it.getColorStateList(R.styleable.AppTextFieldView_iconTint)
            iconGravity = it.getInt(R.styleable.AppTextFieldView_iconGravity, iconGravity)
            icon = it.getDrawable(R.styleable.AppTextFieldView_icon)
        }
        applyRawXmlIconAttributes(attrs)
        applyIcon()
    }

    private fun applyDefaultBackground(attrs: AttributeSet?) {
        val hasXmlBackground = attrs?.getAttributeValue(ANDROID_NS, "background") != null
        if (!hasXmlBackground) {
            background = null
        }
        setPadding(
            resources.getDimensionPixelSize(R.dimen.ds_space_12),
            resources.getDimensionPixelSize(R.dimen.ds_space_6),
            resources.getDimensionPixelSize(R.dimen.ds_space_12),
            resources.getDimensionPixelSize(R.dimen.ds_space_6),
        )
    }

    private fun applyDefaultTextTokens(attrs: AttributeSet?) {
        if (!hasExplicitTextAttribute(attrs, android.R.attr.textColor)) {
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary))
        }
        if (!hasExplicitTextAttribute(attrs, android.R.attr.textColorHint)) {
            setHintTextColor(ContextCompat.getColor(context, R.color.ds_text_muted))
        }
    }

    private fun hasExplicitTextAttribute(attrs: AttributeSet?, attribute: Int): Boolean {
        if (attrs == null) return false
        return context.obtainStyledAttributes(attrs, intArrayOf(attribute)).use { it.hasValue(0) }
    }

    private fun applyMinHeightToken(attrs: AttributeSet?) {
        val tokenHeight = resources.getDimensionPixelSize(R.dimen.ds_input_height)
        val fixedXmlHeight = attrs
            ?.getAttributeResourceValue(ANDROID_NS, "layout_height", 0)
            ?.takeIf { it != 0 }
            ?.let { heightRes ->
                runCatching { resources.getDimensionPixelSize(heightRes) }.getOrNull()
            }

        minHeight = fixedXmlHeight
            ?.takeIf { it in 1 until tokenHeight }
            ?: tokenHeight
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

    private fun shouldForceSingleLine(attrs: AttributeSet?): Boolean {
        if (attrs == null) return true
        if (attrs.getAttributeBooleanValue(ANDROID_NS, "singleLine", true) == false) return false
        val inputType = attrs.getAttributeValue(ANDROID_NS, "inputType").orEmpty()
        return !inputType.contains("textMultiLine", ignoreCase = true)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val ICON_GRAVITY_TEXT_START = 1
        const val ICON_GRAVITY_TEXT_END = 2
    }
}
