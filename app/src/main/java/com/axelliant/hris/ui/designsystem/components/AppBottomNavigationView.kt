package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class AppBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BottomNavigationView(context, attrs, defStyleAttr) {
    init {
        itemActiveIndicatorColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.ds_primary_container)
        )
        itemActiveIndicatorHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._36sdp)
        itemActiveIndicatorWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._68sdp)
        itemActiveIndicatorMarginHorizontal = 0
        itemActiveIndicatorShapeAppearance = ShapeAppearanceModel.builder()
            .setAllCorners(
                CornerFamily.ROUNDED,
                resources.getDimension(com.intuit.sdp.R.dimen._8sdp)
            )
            .build()
        isItemActiveIndicatorEnabled = true
    }
}
