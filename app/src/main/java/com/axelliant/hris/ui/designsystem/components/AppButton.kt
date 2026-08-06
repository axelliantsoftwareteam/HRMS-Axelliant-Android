package com.axelliant.hris.ui.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.microsoft.fluentui.theme.token.controlTokens.ButtonSize
import com.microsoft.fluentui.theme.token.controlTokens.ButtonStyle
import com.microsoft.fluentui.tokenized.controls.Button

enum class AppButtonStyle { Primary, Outline, Subtle, Filled, Outlined, Text }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        size = ButtonSize.Medium,
        style = when (style) {
            AppButtonStyle.Primary, AppButtonStyle.Filled -> ButtonStyle.Button
            AppButtonStyle.Outline, AppButtonStyle.Outlined -> ButtonStyle.OutlinedButton
            AppButtonStyle.Subtle, AppButtonStyle.Text -> ButtonStyle.TextButton
        },
        text = if (loading) "$text..." else text,
        contentDescription = text,
    )
}
