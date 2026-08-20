package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.axelliant.hris.R

/**
 * XML-safe text bridge.
 *
 * Text views are measured by Spinner, RecyclerView pre-layout, PopupWindow, and dialogs before they
 * are always attached to a lifecycle-aware window. A Compose-backed TextView crashes in those paths,
 * so this bridge stays as a real TextView while applying Fluent2 token styles and tones.
 */
class AppTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        includeFontPadding = false
        context.obtainStyledAttributes(attrs, R.styleable.AppTextView, defStyleAttr, 0).use {
            applyTextStyle(it.getInt(R.styleable.AppTextView_fluentTextStyle, TEXT_STYLE_UNSET))
            applyTextTone(it.getInt(R.styleable.AppTextView_fluentTextTone, TEXT_TONE_UNSET))
        }
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(View.LAYOUT_DIRECTION_LTR)
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
    }

    fun setFluentTextStyle(style: Int) {
        applyTextStyle(style)
    }

    fun setFluentTextTone(tone: Int) {
        applyTextTone(tone)
    }

    private fun applyTextStyle(style: Int) {
        val styleRes = when (style) {
            TEXT_STYLE_DISPLAY -> R.style.TextAppearance_Fluent2_Display
            TEXT_STYLE_HEADING -> R.style.TextAppearance_Fluent2_Heading
            TEXT_STYLE_TITLE -> R.style.TextAppearance_Fluent2_Title
            TEXT_STYLE_SUBHEADING -> R.style.TextAppearance_Fluent2_Subheading
            TEXT_STYLE_BODY -> R.style.TextAppearance_Fluent2_Body
            TEXT_STYLE_CAPTION -> R.style.TextAppearance_Fluent2_Caption
            TEXT_STYLE_FORM_LABEL -> R.style.TextAppearance_Fluent2_FormLabel
            else -> return
        }
        setTextAppearance(styleRes)
        includeFontPadding = false
    }

    private fun applyTextTone(tone: Int) {
        val colorRes = when (tone) {
            TEXT_TONE_PRIMARY -> R.color.ds_text_primary
            TEXT_TONE_SECONDARY -> R.color.ds_text_secondary
            TEXT_TONE_MUTED -> R.color.ds_text_muted
            TEXT_TONE_INVERSE -> R.color.ds_neutral_white
            TEXT_TONE_ACCENT -> R.color.ds_primary
            TEXT_TONE_DANGER -> R.color.ds_error
            else -> return
        }
        setTextColor(ContextCompat.getColor(context, colorRes))
    }

    private companion object {
        const val TEXT_STYLE_UNSET = -1
        const val TEXT_STYLE_DISPLAY = 0
        const val TEXT_STYLE_HEADING = 1
        const val TEXT_STYLE_TITLE = 2
        const val TEXT_STYLE_SUBHEADING = 3
        const val TEXT_STYLE_BODY = 4
        const val TEXT_STYLE_CAPTION = 5
        const val TEXT_STYLE_FORM_LABEL = 6

        const val TEXT_TONE_UNSET = -1
        const val TEXT_TONE_PRIMARY = 0
        const val TEXT_TONE_SECONDARY = 1
        const val TEXT_TONE_MUTED = 2
        const val TEXT_TONE_INVERSE = 3
        const val TEXT_TONE_ACCENT = 4
        const val TEXT_TONE_DANGER = 5
    }

}
