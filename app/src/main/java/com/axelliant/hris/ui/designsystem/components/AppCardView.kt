package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.util.AttributeSet
import android.content.res.ColorStateList
import androidx.cardview.widget.CardView
import com.axelliant.hris.R

class AppCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : CardView(context, attrs, defStyleAttr) {
    init {
        radius = resources.getDimension(R.dimen.ia_ds_radius_md)
        cardElevation = resources.getDimension(R.dimen.ds_card_elevation)
    }

    fun setStrokeColor(color: Int) = Unit

    fun setStrokeColor(colors: ColorStateList?) = Unit
}
