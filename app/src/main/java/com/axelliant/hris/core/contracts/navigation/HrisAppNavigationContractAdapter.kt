package com.axelliant.hris.core.contracts.navigation

import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import com.axelliant.hris.R
import com.axelliant.hris.core.contracts.session.SessionContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrisAppNavigationContractAdapter @Inject constructor(
    private val navControllerStore: AppNavControllerStore,
    private val sessionContract: SessionContract
) : AppNavigationContract {
    override fun openWorkspace(workspace: WorkspaceKey) {
        if (workspace != WorkspaceKey.HRIS) return

        if (sessionContract.canAccess(workspace)) {
            openHome(workspace)
        } else {
            openLogin(workspace)
        }
    }

    override fun openLogin(workspace: WorkspaceKey) {
        if (workspace == WorkspaceKey.HRIS) {
            navigate(R.id.loginFragment)
        }
    }

    override fun openHome(workspace: WorkspaceKey) {
        if (workspace == WorkspaceKey.HRIS) {
            navigate(R.id.homeFragment)
        }
    }

    override fun goBack(): Boolean {
        return navControllerStore.current()?.navigateUp() ?: false
    }

    private fun navigate(destination: Int) {
        val navController = navControllerStore.current() ?: return
        if (navController.currentDestination?.id == destination) return

        navController.navigate(
            destination,
            bundleOf(),
            NavOptions.Builder()
                .setPopUpTo(R.id.appEntryFragment, true)
                .build()
        )
    }
}
