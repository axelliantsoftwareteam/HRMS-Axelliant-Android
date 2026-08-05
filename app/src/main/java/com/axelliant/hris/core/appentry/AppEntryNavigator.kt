package com.axelliant.hris.core.appentry

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.axelliant.hris.R
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.utils.SessionManager
import javax.inject.Inject

class AppEntryNavigator @Inject constructor(
    private val sessionManager: SessionManager
) {
    fun isEnabled(workspace: WorkspaceKey): Boolean {
        return workspace == WorkspaceKey.HRIS
    }

    fun navigate(workspace: WorkspaceKey, navController: NavController) {
        if (!isEnabled(workspace)) return

        val destination = if (sessionManager.checkLogin()) {
            R.id.homeFragment
        } else {
            R.id.loginFragment
        }
        navController.navigate(
            destination,
            bundleOf(),
            NavOptions.Builder()
                .setPopUpTo(R.id.appEntryFragment, true)
                .build()
        )
    }
}
