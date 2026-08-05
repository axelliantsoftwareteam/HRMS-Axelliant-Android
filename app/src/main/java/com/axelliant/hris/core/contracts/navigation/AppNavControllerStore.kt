package com.axelliant.hris.core.contracts.navigation

import androidx.navigation.NavController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavControllerStore @Inject constructor() {
    private var navController: NavController? = null

    fun attach(controller: NavController) {
        navController = controller
    }

    fun current(): NavController? = navController
}
