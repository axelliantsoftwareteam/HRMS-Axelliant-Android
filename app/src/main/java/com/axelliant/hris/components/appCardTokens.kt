package com.axelliant.hris.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axelliant.hris.R
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardInfo
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardTokens
import kotlinx.parcelize.Parcelize


@Parcelize
class TodayCardTokens(
    private val gradientStart: Long = 0xFF106EBE,
    private val gradientMid: Long = 0xFF005A9E,
    private val gradientEnd: Long = 0xFF004578,
    private val cornerRadiusRes: Int = R.dimen.ds_radius_md,
    private val elevationValue: Int = 6
) : BasicCardTokens() {

    @Composable
    override fun cornerRadius(basicCardInfo: BasicCardInfo): Dp {
        return dimensionResource(cornerRadiusRes)
    }

    @Composable
    override fun elevation(basicCardInfo: BasicCardInfo): Dp {
        return elevationValue.dp
    }

    @Composable
    override fun backgroundBrush(basicCardInfo: BasicCardInfo): Brush {
        return Brush.verticalGradient(
            colors = listOf(Color(gradientStart),Color(gradientMid), Color(gradientEnd))
        )
    }

    @Composable
    override fun borderColor(basicCardInfo: BasicCardInfo): Brush {
        return SolidColor(Color.Transparent)
    }

    @Composable
    override fun borderStrokeWidth(basicCardInfo: BasicCardInfo): Dp {
        return 0.dp
    }
}
