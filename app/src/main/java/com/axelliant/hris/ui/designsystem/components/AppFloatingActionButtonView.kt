package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
            shape = GradientDrawable.OVAL
            color = fabBackgroundTint
        }
        val mask = ColorDrawable(Color.WHITE)
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
        if (backgroundTintRes != 0) {
            ContextCompat.getColorStateList(context, backgroundTintRes)?.let { fabBackgroundTint = it }
        }

        val tintRes = attrs.getAttributeResourceValue(AUTO_NS, "tint", 0)
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
    }
}
