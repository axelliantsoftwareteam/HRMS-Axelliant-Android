package com.axelliant.hris.ui.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.microsoft.fluentui.tokenized.progress.CircularProgressIndicator
import com.microsoft.fluentui.tokenized.progress.LinearProgressIndicator

@Composable
fun AppCircularProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress == null) {
        CircularProgressIndicator(modifier = modifier)
    } else {
        CircularProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = modifier)
    }
}

@Composable
fun AppLinearProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress == null) {
        LinearProgressIndicator(modifier = modifier)
    } else {
        LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = modifier)
    }
}
