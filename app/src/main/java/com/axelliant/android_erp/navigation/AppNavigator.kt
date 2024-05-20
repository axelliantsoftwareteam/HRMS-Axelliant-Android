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

        fun moveBackToPreviousFragment() {
            getController().popBackStack()
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


        fun navigateToHome(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToHome: $args")
            val navAction = NavAction(R.id.homeFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.home_fragment_action, navAction)
                getController().navigate(R.id.home_fragment_action, args)
            }
        }

        fun navigateToLeaves(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToLeaves: $args")
            val navAction = NavAction(R.id.leavesFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.leaves_fragment_action, navAction)
                getController().navigate(R.id.leaves_fragment_action, args)
            }
        }

        fun navigateToProfile(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToProfile: $args")
            val navAction = NavAction(R.id.profileFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.profile_fragment_action, navAction)
                getController().navigate(R.id.profile_fragment_action, args)
            }
        }

        fun navigateToExpense(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToProfile: $args")
            val navAction = NavAction(R.id.expenseFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.expense_fragment_action, navAction)
                getController().navigate(R.id.expense_fragment_action, args)
            }
        }


        fun navigateToApplyLeaves(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToProfile: $args")
            val navAction = NavAction(R.id.applyLeavesFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.apply_leaves_fragment_action, navAction)
                getController().navigate(R.id.apply_leaves_fragment_action, args)
            }
        }

        fun navigateToAttendanceStats(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToAttendanceStats: $args")
            val navAction = NavAction(R.id.attendanceStatsFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.attendance_stats_fragment_action, navAction)
                getController().navigate(R.id.attendance_stats_fragment_action, args)
            }
        }

        fun navigateToMyAttendanceDetail(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToAttendanceStats: $args")
            val navAction = NavAction(R.id.myAttendanceDetailFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.my_attendance_detail_fragment_action, navAction)
                getController().navigate(R.id.my_attendance_detail_fragment_action, args)
            }


        }


        fun navigateToTeamAttendanceDetail(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToTeamAttendanceStats: $args")
            val navAction = NavAction(R.id.teamAttendanceDetailFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.team_attendance_detail_fragment_action, navAction)
                getController().navigate(R.id.team_attendance_detail_fragment_action, args)
            }


        }

    }
}
