package com.axelliant.hris.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardInfo
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardTokens
import kotlinx.parcelize.Parcelize
import com.axelliant.hris.R

@Parcelize
class LeaveCardTokens(
    private val radius: Int = R.dimen.ds_radius_md
) : BasicCardTokens() {

    @Composable
    override fun cornerRadius(basicCardInfo: BasicCardInfo): Dp {
        return dimensionResource(id = radius)
    }
}
