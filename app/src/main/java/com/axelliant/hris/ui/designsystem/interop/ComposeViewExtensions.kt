package com.axelliant.hris.ui.designsystem.interop

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.axelliant.hris.ui.designsystem.theme.AppFluentTheme

fun ComposeView.setFluentContent(content: @Composable () -> Unit) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        AppFluentTheme {
            content()
        }
    }
}
