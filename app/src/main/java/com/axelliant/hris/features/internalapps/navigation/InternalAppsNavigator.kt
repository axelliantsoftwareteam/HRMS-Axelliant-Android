package com.axelliant.hris.features.internalapps.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.axelliant.hris.R

object InternalAppsNavigator {
    fun open(
        navController: NavController,
        @IdRes destinationId: Int,
        args: Bundle? = null
    ) {
        if (navController.currentDestination?.id == destinationId) return

        navController.navigate(
            destinationId,
            args,
            NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build()
        )
    }

    fun returnToHomeShell(navController: NavController) {
        if (navController.popBackStack(R.id.homeFragment, false)) return

        navController.navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, false)
                .setLaunchSingleTop(true)
                .build()
        )
    }

    fun openLogin(navController: NavController) {
        navController.navigate(
            R.id.commonLoginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, true)
                .build()
        )
    }
}
