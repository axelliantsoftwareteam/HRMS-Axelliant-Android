package com.axelliant.hris.features.dashboard.presentation

import com.axelliant.hris.core.AppDrawerAction

data class AppDrawerMenuItem(
    val titleRes: Int,
    val iconRes: Int,
    val action: AppDrawerAction,
    val isDestructive: Boolean = false
)
