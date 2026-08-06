package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.content.res.ColorStateList
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.axelliant.hris.R

class AppTextFieldLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    var error: CharSequence? = null
        set(value) {
            field = value
            isErrorEnabled = !value.isNullOrBlank()
            updateErrorText()
        }
    var hint: CharSequence? = null
        set(value) {
            field = value
            editText?.hint = value
        }
    var isErrorEnabled: Boolean = false
    var isEndIconVisible: Boolean = false
    var endIconMode: Int = END_ICON_NONE
    var endIconDrawable: Drawable? = null
    var isEndIconCheckable: Boolean = false
    private var endIconTint: Int? = null
    private var errorTextColor: ColorStateList? = null
    private var passwordVisible = false
    private val errorTextView = AppCompatTextView(context)
    val editText: AppTextFieldView?
        get() = (0 until childCount).map { getChildAt(it) }.filterIsInstance<AppTextFieldView>().firstOrNull()

    init {
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        orientation = VERTICAL
        applyDefaultBackground(attrs)
        applyRawXmlAttributes(attrs)
        setupErrorText()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ensureErrorTextView()
        applyEndIcon()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(View.LAYOUT_DIRECTION_LTR)
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        applyEndIcon()
    }

    fun setEndIconOnClickListener(listener: OnClickListener?) {
        editText?.setOnClickListener(listener)
    }

    private fun applyDefaultBackground(attrs: AttributeSet?) {
        val hasXmlBackground = attrs?.getAttributeValue(
            ANDROID_NS,
            "background"
        ) != null

        if (!hasXmlBackground) {
            setBackgroundResource(R.drawable.bg_filter_field)
        }
    }

    private fun applyRawXmlAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        when (attrs.getAttributeValue(AUTO_NS, "endIconMode")) {
            "password_toggle" -> endIconMode = END_ICON_PASSWORD_TOGGLE
            "clear_text" -> endIconMode = END_ICON_CLEAR_TEXT
            "custom" -> endIconMode = END_ICON_CUSTOM
        }

        attrs.getAttributeResourceValue(AUTO_NS, "endIconTint", 0)
            .takeIf { it != 0 }
            ?.let { endIconTint = ContextCompat.getColor(context, it) }

        attrs.getAttributeResourceValue(AUTO_NS, "errorTextColor", 0)
            .takeIf { it != 0 }
            ?.let { errorTextColor = ContextCompat.getColorStateList(context, it) }
    }

    private fun setupErrorText() {
        errorTextView.visibility = GONE
        errorTextView.includeFontPadding = false
        errorTextView.textSize = 11f
        errorTextView.setTextColor(
            errorTextColor ?: ContextCompat.getColorStateList(context, R.color.login_error)
        )
        errorTextView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.ds_space_4)
        }
    }

    private fun updateErrorText() {
        ensureErrorTextView()
        errorTextView.text = error ?: ""
        errorTextView.visibility = if (isErrorEnabled) VISIBLE else GONE
    }

    private fun ensureErrorTextView() {
        if (errorTextView.parent == null) {
            addView(errorTextView)
        }
    }

    private fun applyEndIcon() {
        val field = editText ?: return
        if (endIconMode != END_ICON_PASSWORD_TOGGLE) return

        endIconDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_login_eye)?.mutate()?.let {
            DrawableCompat.wrap(it).also { wrapped ->
                endIconTint?.let { tint -> DrawableCompat.setTint(wrapped, tint) }
                wrapped.setBounds(0, 0, wrapped.intrinsicWidth, wrapped.intrinsicHeight)
            }
        }

        updatePasswordToggle(field)
        field.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP || !isTouchOnEndIcon(field, event)) return@setOnTouchListener false
            passwordVisible = !passwordVisible
            updatePasswordToggle(field)
            field.setSelection(field.text?.length ?: 0)
            true
        }
    }

    private fun updatePasswordToggle(field: AppTextFieldView) {
        val drawables = field.compoundDrawables
        field.setCompoundDrawables(drawables[0], drawables[1], endIconDrawable, drawables[3])
        field.transformationMethod = if (passwordVisible) {
            SingleLineTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        field.inputType = field.inputType or InputType.TYPE_CLASS_TEXT
    }

    private fun isTouchOnEndIcon(field: AppTextFieldView, event: MotionEvent): Boolean {
        val drawable = field.compoundDrawables[2] ?: return false
        val iconStart = field.width - field.paddingEnd - drawable.bounds.width()
        return event.x >= iconStart
    }

    companion object {
        private const val ANDROID_NS =
            "http://schemas.android.com/apk/res/android"

        private const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val END_ICON_NONE = 0
        const val END_ICON_CLEAR_TEXT = 1
        const val END_ICON_CUSTOM = 2
        const val END_ICON_PASSWORD_TOGGLE = 3
    }
}
