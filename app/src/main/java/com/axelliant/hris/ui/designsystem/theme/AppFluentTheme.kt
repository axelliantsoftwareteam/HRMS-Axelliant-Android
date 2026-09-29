package com.axelliant.hris.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.ThemeMode

/**
 * Single application entry point for Microsoft Fluent UI Compose (Fluent 2).
 *
 * Keep MaterialComponents as the Android XML theme while legacy XML screens are
 * migrated. Compose content must be wrapped in this theme.
 */
@Composable
fun AppFluentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    FluentTheme(
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
        content = content,
    )
}
