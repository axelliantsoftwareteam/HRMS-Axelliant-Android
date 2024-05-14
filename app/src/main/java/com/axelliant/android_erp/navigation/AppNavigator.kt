package com.axelliant.android_erp.navigation

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavAction
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.axelliant.android_erp.R
import com.axelliant.android_erp.config.GlobalConfig


class AppNavigator {
    companion object {
        private const val TAG = "Navigator"
        private fun getController(): NavController {
            return GlobalConfig.getInstance().navController
        }

        fun getCurrentDestinationId(): Int? {
            return getController().currentDestination?.id
        }

        fun navigateToSplash(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToLogin: $args")
            val navAction = NavAction(R.id.splashFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, true).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.splash_fragment_action, navAction)
                getController().navigate(R.id.splash_fragment_action, args)
            }
        }

        fun navigateToLogin(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToLogin: $args")
            val navAction = NavAction(R.id.loginFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, true).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.login_fragment_action, navAction)
                getController().navigate(R.id.login_fragment_action, args)
            }
        }



    }
}
