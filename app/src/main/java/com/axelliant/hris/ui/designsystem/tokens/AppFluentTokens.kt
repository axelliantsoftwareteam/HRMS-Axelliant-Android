package com.axelliant.hris.ui.designsystem.tokens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axelliant.hris.R
import com.microsoft.fluentui.theme.token.StateBrush
import com.microsoft.fluentui.theme.token.StateColor
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardInfo
import com.microsoft.fluentui.theme.token.controlTokens.BasicCardTokens
import com.microsoft.fluentui.theme.token.controlTokens.ButtonInfo
import com.microsoft.fluentui.theme.token.controlTokens.ButtonTokens
import kotlinx.parcelize.Parcelize

@Parcelize
class CheckInButtonTokens(
    private val fontSizeDimenName: String = "_16sdp",
    private val iconSizeDimenName: String = "_35sdp",
    private val bgRest: Long = 0xFF0078D4,
    private val bgPressed: Long = 0xFF106EBE,
    private val bgSelected: Long = 0xFF106EBE,
    private val bgFocused: Long = 0xFF272757,
    private val bgDisabled: Long = 0xFFACACAC,
    private val textRest: Long = 0xFFFFFFFF,
    private val textPressed: Long = 0xFFFFFFFF,
    private val textSelected: Long = 0xFFFFFFFF,
    private val textFocused: Long = 0xFFFFFFFF,
    private val textDisabled: Long = 0xFF999999,
    private val iconRest: Long = 0xFFFFFFFF,
    private val iconPressed: Long = 0xFFFFFFFF,
    private val iconSelected: Long = 0xFFFFFFFF,
    private val iconFocused: Long = 0xFFFFFFFF,
    private val iconDisabled: Long = 0xFF9E9E9E,
) : ButtonTokens() {

    @Composable
    override fun typography(buttonInfo: ButtonInfo): TextStyle {
        val context = LocalContext.current
        val density = LocalDensity.current
        val sdpId = context.resources.getIdentifier(fontSizeDimenName, "dimen", context.packageName)
        val scalableFontSize = if (sdpId != 0) {
            val pixelSize = context.resources.getDimension(sdpId)
            with(density) { pixelSize.toSp() }
        } else {
            16.sp
        }
        return TextStyle(fontSize = scalableFontSize)
    }

    @Composable
    override fun iconSize(buttonInfo: ButtonInfo): Dp {
        val context = LocalContext.current
        val density = LocalDensity.current
        val sdpId = context.resources.getIdentifier(iconSizeDimenName, "dimen", context.packageName)
        return if (sdpId != 0) {
            val pixelSize = context.resources.getDimension(sdpId)
            with(density) { pixelSize.toDp() }
        } else {
            35.dp
        }
    }

    @Composable
    override fun cornerRadius(buttonInfo: ButtonInfo): Dp = 4.dp

    @Composable
    override fun backgroundBrush(buttonInfo: ButtonInfo): StateBrush {
        return StateBrush(
            rest = SolidColor(Color(bgRest)),
            pressed = SolidColor(Color(bgPressed)),
            selected = SolidColor(Color(bgSelected)),
            focused = SolidColor(Color(bgFocused)),
            disabled = SolidColor(Color(bgDisabled)),
        )
    }

    @Composable
    override fun textColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(textRest),
            pressed = Color(textPressed),
            selected = Color(textSelected),
            focused = Color(textFocused),
            disabled = Color(textDisabled),
        )
    }

    @Composable
    override fun iconColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(iconRest),
            pressed = Color(iconPressed),
            selected = Color(iconSelected),
            focused = Color(iconFocused),
            disabled = Color(iconDisabled),
        )
    }
}

@Parcelize
class DestructiveButtonTokens(
    private val bgRest: Long = 0xFFD83242,
    private val bgPressed: Long = 0xFFC50F1F,
    private val bgSelected: Long = 0xFFC50F1F,
    private val bgFocused: Long = 0xFFD83242,
    private val bgDisabled: Long = 0xFFACACAC,
    private val textRest: Long = 0xFFFFFFFF,
    private val textPressed: Long = 0xFFFFFFFF,
    private val textSelected: Long = 0xFFFFFFFF,
    private val textFocused: Long = 0xFFFFFFFF,
    private val textDisabled: Long = 0xFF666666,
    private val iconRest: Long = 0xFFFFFFFF,
    private val iconPressed: Long = 0xFFFFFFFF,
    private val iconSelected: Long = 0xFFFFFFFF,
    private val iconFocused: Long = 0xFFFFFFFF,
    private val iconDisabled: Long = 0xFF666666,
) : ButtonTokens() {

    @Composable
    override fun cornerRadius(buttonInfo: ButtonInfo): Dp = dimensionResource(R.dimen.ds_radius_md)

    @Composable
    override fun backgroundBrush(buttonInfo: ButtonInfo): StateBrush {
        return StateBrush(
            rest = SolidColor(Color(bgRest)),
            pressed = SolidColor(Color(bgPressed)),
            selected = SolidColor(Color(bgSelected)),
            focused = SolidColor(Color(bgFocused)),
            disabled = SolidColor(Color(bgDisabled)),
        )
    }

    @Composable
    override fun textColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(textRest),
            pressed = Color(textPressed),
            selected = Color(textSelected),
            focused = Color(textFocused),
            disabled = Color(textDisabled),
        )
    }

    @Composable
    override fun iconColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(iconRest),
            pressed = Color(iconPressed),
            selected = Color(iconSelected),
            focused = Color(iconFocused),
            disabled = Color(iconDisabled),
        )
    }
}

@Parcelize
class TodayCardTokens(
    private val gradientStart: Long = 0xFF106EBE,
    private val gradientMid: Long = 0xFF005A9E,
    private val gradientEnd: Long = 0xFF004578,
    private val cornerRadiusRes: Int = R.dimen.ds_radius_md,
    private val elevationValue: Int = 6,
) : BasicCardTokens() {

    @Composable
    override fun cornerRadius(basicCardInfo: BasicCardInfo): Dp = dimensionResource(cornerRadiusRes)

    @Composable
    override fun elevation(basicCardInfo: BasicCardInfo): Dp = elevationValue.dp

    @Composable
    override fun backgroundBrush(basicCardInfo: BasicCardInfo): Brush {
        return Brush.verticalGradient(
            colors = listOf(Color(gradientStart), Color(gradientMid), Color(gradientEnd)),
        )
    }

    @Composable
    override fun borderColor(basicCardInfo: BasicCardInfo): Brush = SolidColor(Color.Transparent)

    @Composable
    override fun borderStrokeWidth(basicCardInfo: BasicCardInfo): Dp = 0.dp
}

@Parcelize
class StatCardTokens(
    private val radius: Int = R.dimen.ds_radius_md,
) : BasicCardTokens() {

    @Composable
    override fun cornerRadius(basicCardInfo: BasicCardInfo): Dp = dimensionResource(id = radius)
}

@Parcelize
class LeaveCardTokens(
    private val radius: Int = R.dimen.ds_radius_md,
) : BasicCardTokens() {

    @Composable
    override fun cornerRadius(basicCardInfo: BasicCardInfo): Dp = dimensionResource(id = radius)
}
