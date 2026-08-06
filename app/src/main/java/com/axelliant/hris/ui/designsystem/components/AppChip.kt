package com.axelliant.hris.ui.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.microsoft.fluentui.tokenized.controls.BasicChip

@Composable
fun AppChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    BasicChip(
        label = label,
        modifier = modifier,
        enabled = enabled,
        selected = selected,
        onClick = onClick,
    )
}
