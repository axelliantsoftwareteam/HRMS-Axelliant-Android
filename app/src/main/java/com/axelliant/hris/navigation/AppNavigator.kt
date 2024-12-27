package com.axelliant.hris.navigation

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavAction
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.axelliant.hris.R
import com.axelliant.hris.config.GlobalConfig


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

        fun navigateToApprovals(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToLeaves: $args")
            val navAction = NavAction(R.id.approvalFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.approvals_fragment_action, navAction)
                getController().navigate(R.id.approvals_fragment_action, args)
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


        fun navigateToRequest(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToRequest: $args")
            val navAction = NavAction(R.id.requestFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.request_fragment_action, navAction)
                getController().navigate(R.id.request_fragment_action, args)
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


        fun navigateToMyLeaveDetail(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToMyLeaveDetail: $args")
            val navAction = NavAction(R.id.myLeaveDetailFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.my_leave_detail_fragment_action, navAction)
                getController().navigate(R.id.my_leave_detail_fragment_action, args)
            }


        }

        fun navigateToTeamLeaveDetail(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToTeamLeaveDetail: $args")
            val navAction = NavAction(R.id.teamLeaveDetailFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.team_leave_detail_fragment_action, navAction)
                getController().navigate(R.id.team_leave_detail_fragment_action, args)
            }


        }


        fun navigateToCheckInFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToCheckInFragment: $args")
            val navAction = NavAction(R.id.checkInListFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.check_in_list_fragment_action, navAction)
                getController().navigate(R.id.check_in_list_fragment_action, args)
            }

        }
        fun navigateToExpenseFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToExpenseFragment: $args")
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

        fun navigateToAddExpenseFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToAddExpenseFragment: $args")
            val navAction = NavAction(R.id.addExpenseFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.add_expense_fragment_action, navAction)
                getController().navigate(R.id.add_expense_fragment_action, args)
            }

        }
        fun navigateToImageDetailFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToImageDetailFragment: $args")
            val navAction = NavAction(R.id.imageDetailFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.image_detail_fragment_action, navAction)
                getController().navigate(R.id.image_detail_fragment_action, args)
            }

        }
        fun navigateToDocumentManageFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToDocumentManageFragment: $args")
            val navAction = NavAction(R.id.documentManageFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.document_manage_fragment_action, navAction)
                getController().navigate(R.id.document_manage_fragment_action, args)
            }

        }

        fun navigateToAddDocumentFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToAddDocumentFragment: $args")
            val navAction = NavAction(R.id.addDocumentFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.add_document_fragment_action, navAction)
                getController().navigate(R.id.add_document_fragment_action, args)
            }

        }
        fun navigateToResourceManageFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToResourceManageFragment: $args")
            val navAction = NavAction(R.id.resourceManageFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.resource_manage_fragment_action, navAction)
                getController().navigate(R.id.resource_manage_fragment_action, args)
            }

        }

        fun navigateToAddResourceManageFragment(args: Bundle = Bundle()) {
            Log.i(TAG, "navigateToAddResourceManageFragment: $args")
            val navAction = NavAction(R.id.addResourceManageFragment)
            val navOptions = NavOptions.Builder()
                .setPopUpTo(getCurrentDestinationId()!!, false).build()
            navAction.navOptions = navOptions

            val destination: NavDestination? = getCurrentDestinationId()?.let {
                getController().graph.findNode(it)
            }
            if (destination != null) {
                destination.putAction(R.id.add_resource_manage_fragment_action, navAction)
                getController().navigate(R.id.add_resource_manage_fragment_action, args)
            }

        }

    }
}
