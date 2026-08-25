package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.axelliant.hris.R

class AppFloatingActionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.imageButtonStyle,
) : AppCompatImageButton(context, attrs, defStyleAttr) {
    private var isStateReady = false
    private var fabSize: Int = FAB_SIZE_NORMAL
    private var fabBackgroundTint: ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ds_primary))
    private var fabRippleTint: ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ds_primary_pressed))

    init {
        scaleType = ScaleType.CENTER
        contentDescription = contentDescription ?: ""
        isClickable = true
        isFocusable = true
        clipToOutline = true
        elevation = resources.getDimension(com.intuit.sdp.R.dimen._6sdp)
        ViewCompat.setElevation(this, elevation)

        readXmlAttributes(attrs)
        background = createCircleBackground()
        isStateReady = true
    }

    override fun setBackgroundTintList(tint: ColorStateList?) {
        if (!isStateReady) {
            super.setBackgroundTintList(tint)
            return
        }
        if (tint == null) {
            super.setBackgroundTintList(null)
            return
        }
        fabBackgroundTint = tint
        background = createCircleBackground()
    }

    override fun setImageResource(resId: Int) {
        setImageDrawable(AppCompatResources.getDrawable(context, resId))
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = resources.getDimensionPixelSize(
            if (fabSize == FAB_SIZE_MINI) com.intuit.sdp.R.dimen._40sdp else com.intuit.sdp.R.dimen._56sdp
        )
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        )
    }

    private fun createCircleBackground(): RippleDrawable {
        val oval = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.ds_radius_xl)
            color = fabBackgroundTint
        }
        val mask = ColorDrawable(ContextCompat.getColor(context, R.color.ds_neutral_white))
        return RippleDrawable(fabRippleTint, oval, mask)
    }

    private fun readXmlAttributes(attrs: AttributeSet?) {
        if (attrs == null) {
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ds_on_primary))
            return
        }

        fabSize = when (attrs.getAttributeValue(AUTO_NS, "fabSize")) {
            "mini" -> FAB_SIZE_MINI
            else -> FAB_SIZE_NORMAL
        }

        val backgroundTintRes = attrs.getAttributeResourceValue(AUTO_NS, "backgroundTint", 0)
            .takeIf { it != 0 }
            ?: attrs.getAttributeResourceValue(ANDROID_NS, "backgroundTint", 0)
        if (backgroundTintRes != 0) {
            ContextCompat.getColorStateList(context, backgroundTintRes)?.let { fabBackgroundTint = it }
        }

        val rippleTintRes = attrs.getAttributeResourceValue(AUTO_NS, "rippleColor", 0)
        if (rippleTintRes != 0) {
            ContextCompat.getColorStateList(context, rippleTintRes)?.let { fabRippleTint = it }
        }

        val elevationRes = attrs.getAttributeResourceValue(AUTO_NS, "elevation", 0)
        if (elevationRes != 0) {
            elevation = resources.getDimension(elevationRes)
            ViewCompat.setElevation(this, elevation)
        }

        val tintRes = attrs.getAttributeResourceValue(AUTO_NS, "tint", 0)
            .takeIf { it != 0 }
            ?: attrs.getAttributeResourceValue(ANDROID_NS, "tint", 0)
        imageTintList = if (tintRes != 0) {
            ContextCompat.getColorStateList(context, tintRes)
        } else {
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.ds_on_primary))
        }

        if (drawable != null) return

        val srcCompat = attrs.getAttributeResourceValue(AUTO_NS, "srcCompat", 0)
        val androidSrc = attrs.getAttributeResourceValue(ANDROID_NS, "src", 0)
        val imageRes = if (srcCompat != 0) srcCompat else androidSrc
        if (imageRes != 0) {
            setImageDrawable(AppCompatResources.getDrawable(context, imageRes))
        }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val FAB_SIZE_NORMAL = 0
        const val FAB_SIZE_MINI = 1
        const val ENABLED_ALPHA = 1f
        const val DISABLED_ALPHA = 0.45f
    }
}
