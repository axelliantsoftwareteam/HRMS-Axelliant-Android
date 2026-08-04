package com.axelliant.hris.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.microsoft.fluentui.theme.token.controlTokens.ButtonInfo
import com.microsoft.fluentui.theme.token.controlTokens.ButtonTokens
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.microsoft.fluentui.theme.token.StateBrush
import com.microsoft.fluentui.theme.token.StateColor
import androidx.compose.ui.res.dimensionResource
import com.axelliant.hris.R

@Parcelize
class CheckInButtonTokens(
    // text style
    private val fontSizeDimenName: String = "_16sdp",
    private val iconSizeDimenName: String = "_35sdp",
    // background
    private val bgRest: Long = 0xFF0078D4,
    private val bgPressed: Long = 0xFF106EBE,
    private val bgSelected: Long = 0xFF106EBE,
    private val bgFocused: Long = 0xFF272757,
    private val bgDisabled: Long = 0xFFACACAC,
    // text color
    private val textRest: Long = 0xFFFFFFFF,
    private val textPressed: Long = 0xFFFFFFFF,
    private val textSelected: Long = 0xFFFFFFFF,
    private val textFocused: Long = 0xFFFFFFFF,
    private val textDisabled: Long = 0xFF999999,
    // icon color
    private val iconRest: Long = 0xFFFFFFFF,
    private val iconPressed: Long = 0xFFFFFFFF,
    private val iconSelected: Long = 0xFFFFFFFF,
    private val iconFocused: Long = 0xFFFFFFFF,
    private val iconDisabled: Long = 0xFF9E9E9E
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
        return TextStyle(
            fontSize = scalableFontSize
        )
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
    override fun cornerRadius(buttonInfo: ButtonInfo): Dp {
        return 4.dp
    }


    @Composable
    override fun backgroundBrush(buttonInfo: ButtonInfo): StateBrush {
        return StateBrush(
            rest = SolidColor(Color(bgRest)),
            pressed = SolidColor(Color(bgPressed)),
            selected = SolidColor(Color(bgSelected)),
            focused = SolidColor(Color(bgFocused)),
            disabled = SolidColor(Color(bgDisabled))
        )
    }

    @Composable
    override fun textColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(textRest),
            pressed = Color(textPressed),
            selected = Color(textSelected),
            focused = Color(textFocused),
            disabled = Color(textDisabled)
        )
    }

    @Composable
    override fun iconColor(buttonInfo: ButtonInfo): StateColor {
        return StateColor(
            rest = Color(iconRest),
            pressed = Color(iconPressed),
            selected = Color(iconSelected),
            focused = Color(iconFocused),
            disabled = Color(iconDisabled)
        )
    }
}