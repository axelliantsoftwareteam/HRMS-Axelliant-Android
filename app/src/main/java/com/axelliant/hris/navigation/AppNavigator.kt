package com.axelliant.hris.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavOptions
import com.axelliant.hris.R
import com.axelliant.hris.config.GlobalConfig

/**
 * Single place that moves between screens. Every destination lives in `nav_graph.xml`; this class
 * navigates straight to a destination id (no per-screen action ids), pushing it on the back stack.
 * Login and Home reset the stack so Back never returns to a signed-out or splash screen.
 */
class AppNavigator {
    companion object {

        private fun controller() = GlobalConfig.getInstance().navController

        fun getCurrentDestinationId(): Int? = controller().currentDestination?.id

        fun moveBackToPreviousFragment() {
            controller().popBackStack()
        }

        /** Pushes [destination] on top of the current screen (ignored when it is already showing). */
        private fun push(@IdRes destination: Int, args: Bundle = Bundle()) {
            val nav = controller()
            if (nav.currentDestination?.id == destination) return
            nav.navigate(destination, args)
        }

        /** Navigates to [destination] after clearing the whole back stack. */
        private fun resetTo(@IdRes destination: Int, args: Bundle = Bundle()) {
            val nav = controller()
            val options = NavOptions.Builder().setPopUpTo(nav.graph.id, true).build()
            nav.navigate(destination, args, options)
        }

        fun navigateToSplash(args: Bundle = Bundle()) = resetTo(R.id.splashFragment, args)

        fun navigateToLogin(args: Bundle = Bundle()) {
            if (controller().currentDestination?.id == R.id.loginFragment) return
            resetTo(R.id.loginFragment, args)
        }

        fun navigateToHome(args: Bundle = Bundle()) {
            if (controller().currentDestination?.id == R.id.homeFragment) return
            resetTo(R.id.homeFragment, args)
        }

        fun navigateToLeaves(args: Bundle = Bundle()) = push(R.id.leavesFragment, args)

        fun navigateToApprovals(args: Bundle = Bundle()) = push(R.id.approvalFragment, args)

        fun navigateToExpenseApprovals(args: Bundle = Bundle()) = push(R.id.expenseApprovalsFragment, args)

        fun navigateToProfile(args: Bundle = Bundle()) = push(R.id.profileFragment, args)

        fun navigateToExpense(args: Bundle = Bundle()) = push(R.id.expenseFragment, args)

        fun navigateToAddExpenseFragment(args: Bundle = Bundle()) = push(R.id.addExpenseFragment, args)

        fun navigateToImageDetailFragment(args: Bundle = Bundle()) = push(R.id.imageDetailFragment, args)

        /** Leave / attendance request form (create or edit, see `AppConst.RequestType`). */
        fun navigateToRequest(args: Bundle = Bundle()) = push(R.id.requestFragment, args)

        /** Requests overview (leave + check-in + expense activity). */
        fun navigateToRequestsOverview(args: Bundle = Bundle()) = push(R.id.requestsOverviewFragment, args)

        /** The attendance list; pass `AppConst.KEY_ID` for another employee, none for the signed-in user. */
        fun navigateToMyAttendanceDetail(args: Bundle = Bundle()) = push(R.id.myAttendanceDetailFragment, args)

        fun navigateToPayslips(args: Bundle = Bundle()) = push(R.id.payslipsFragment, args)

        fun navigateToCertifications(args: Bundle = Bundle()) = push(R.id.certificationsFragment, args)

        fun navigateToNotifications(args: Bundle = Bundle()) = push(R.id.notificationsFragment, args)

        fun navigateToTeamActivity(args: Bundle = Bundle()) = push(R.id.teamActivityFragment, args)

        fun navigateToTeamCalendar(args: Bundle = Bundle()) = push(R.id.teamCalendarFragment, args)

        fun navigateToOrganogram(args: Bundle = Bundle()) = push(R.id.organogramFragment, args)
    }
}
