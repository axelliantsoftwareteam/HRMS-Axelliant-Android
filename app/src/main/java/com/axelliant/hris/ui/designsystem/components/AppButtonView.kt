package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LocaleSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import java.util.Locale
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.drawable.DrawableCompat
import com.axelliant.hris.R

class AppButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle,
) : AppCompatButton(context, attrs, defStyleAttr) {
    var insetTop: Int = 0
    var insetBottom: Int = 0
    var cornerRadius: Int = 0
    var strokeWidth: Int = 0
    var strokeColor: ColorStateList? = null
    private var rawText: CharSequence? = null
    private var isRenderingText = false
    var iconTint: ColorStateList? = null
        set(value) {
            field = value
            applyIcon()
        }
    var iconPadding: Int = 0
        set(value) {
            field = value
            compoundDrawablePadding = value
        }
    var iconGravity: Int = ICON_GRAVITY_TEXT_START
        set(value) {
            field = value
            applyIcon()
        }
    var icon: Drawable? = null
        set(value) {
            field = value
            applyIcon()
        }
    var noBackground: Boolean = false
        set(value) {
            field = value
            applyBackgroundMode()
        }

    init {
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        minHeight = resources.getDimensionPixelSize(R.dimen.ds_button_height)
        includeFontPadding = false
        isAllCaps = false
        gravity = Gravity.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER
        context.obtainStyledAttributes(attrs, R.styleable.AppButtonView, defStyleAttr, 0).use {
            iconPadding = it.getDimensionPixelSize(R.styleable.AppButtonView_iconPadding, iconPadding)
            iconTint = it.getColorStateList(R.styleable.AppButtonView_iconTint)
            iconGravity = it.getInt(R.styleable.AppButtonView_iconGravity, iconGravity)
            noBackground = it.getBoolean(R.styleable.AppButtonView_noBackground, noBackground)
            icon = it.getDrawable(R.styleable.AppButtonView_icon)
        }
        applyRawXmlIconAttributes(attrs)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(View.LAYOUT_DIRECTION_LTR)
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        applyIcon()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (!isRenderingText) {
            rawText = text
        }
        super.setText(text, type)
        if (!isRenderingText) {
            applyIcon()
        }
    }

    fun setIconResource(resId: Int) {
        icon = AppCompatResources.getDrawable(context, resId)
    }

    private fun applyIcon() {
        val label = rawText?.toString().orEmpty()
        val isIconOnly = label.isBlank()
        val drawable = icon?.mutate()?.let { source ->
            DrawableCompat.wrap(source).also { wrapped ->
                if (!(isIconOnly && iconPadding == 0)) {
                    iconTint?.let { DrawableCompat.setTintList(wrapped, it) }
                }
            }
        }

        gravity = Gravity.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER

        if (drawable != null && label.isNotBlank()) {
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            setInlineIconText(label, drawable)
        } else if (iconGravity == ICON_GRAVITY_TEXT_END) {
            setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }

        if (isIconOnly && iconPadding == 0) {
            noBackground = true
        }
    }

    private fun setInlineIconText(label: String, drawable: Drawable) {
        val iconToken = " "
        val separator = " "
        val builder = SpannableStringBuilder()
        val iconStart: Int

        if (iconGravity == ICON_GRAVITY_TEXT_END) {
            builder.append(label)
            builder.append(separator)
            iconStart = builder.length
            builder.append(iconToken)
        } else {
            iconStart = 0
            builder.append(iconToken)
            builder.append(separator)
            builder.append(label)
        }

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        builder.setSpan(
            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
            iconStart,
            iconStart + iconToken.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            LocaleSpan(Locale.ENGLISH),
            0,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        isRenderingText = true
        super.setText(builder, BufferType.SPANNABLE)
        isRenderingText = false
    }

    private fun applyBackgroundMode() {
        if (!noBackground) return

        background = null
        backgroundTintList = null
        stateListAnimator = null
        elevation = 0f
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

        noBackground = attrs.getAttributeBooleanValue(AUTO_NS, "noBackground", noBackground)

        when (attrs.getAttributeValue(AUTO_NS, "iconGravity")) {
            "textEnd", "end" -> iconGravity = ICON_GRAVITY_TEXT_END
            "textStart", "start" -> iconGravity = ICON_GRAVITY_TEXT_START
        }
    }

    private companion object {
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val ICON_GRAVITY_TEXT_START = 1
        const val ICON_GRAVITY_TEXT_END = 2
    }
}
