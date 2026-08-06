package com.axelliant.hris.ui.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.microsoft.fluentui.theme.token.controlTokens.FABSize
import com.microsoft.fluentui.theme.token.controlTokens.FABState
import com.microsoft.fluentui.tokenized.controls.FloatingActionButton

@Composable
fun AppFloatingActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = FABSize.Large,
        state = if (text == null) FABState.Collapsed else FABState.Expanded,
        icon = icon,
        text = text,
    )
}
