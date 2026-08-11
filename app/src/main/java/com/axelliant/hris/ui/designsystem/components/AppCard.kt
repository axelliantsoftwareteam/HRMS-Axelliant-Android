package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.google.android.material.card.MaterialCardView

/**
 * XML bridge for card containers.
 * XML children need a real ViewGroup, so this keeps layout control in XML while using app design tokens.
 */
class AppCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {
    init {
        if (!hasXmlCardBackgroundColor(attrs)) {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.ds_surface))
        }
        if (!hasXmlCardCornerRadius(attrs)) {
            radius = resources.getDimension(R.dimen.ds_radius_lg)
        }
        if (!hasXmlCardElevation(attrs)) {
            cardElevation = resources.getDimension(R.dimen.ds_card_elevation)
        }
        if (!hasXmlStrokeColor(attrs)) {
            setStrokeColor(ContextCompat.getColor(context, R.color.ds_outline))
        }
        if (!hasXmlStrokeWidth(attrs)) {
            strokeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setCardStrokeColor(colors: ColorStateList?) {
        strokeColor = colors?.defaultColor ?: ContextCompat.getColor(context, R.color.ds_outline)
    }

    private fun hasXmlCardCornerRadius(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeValue(AUTO_NS, "cardCornerRadius") != null
    }

    private fun hasXmlCardElevation(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeValue(AUTO_NS, "cardElevation") != null
    }

    private fun hasXmlCardBackgroundColor(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeValue(AUTO_NS, "cardBackgroundColor") != null
    }

    private fun hasXmlStrokeColor(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeValue(AUTO_NS, "strokeColor") != null
    }

    private fun hasXmlStrokeWidth(attrs: AttributeSet?): Boolean {
        return attrs?.getAttributeValue(AUTO_NS, "strokeWidth") != null
    }

    private companion object {
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
    }
}
