package com.axelliant.hris.core.appentry

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.axelliant.hris.R
import com.axelliant.hris.core.auth.PendingCommonLoginStore
import com.axelliant.hris.core.contracts.auth.WorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import javax.inject.Inject

class AppEntryNavigator @Inject constructor(
    private val sessionProvider: WorkspaceSessionProvider,
    private val authSessionRepositoryProvider: WorkspaceAuthSessionRepositoryProvider,
    private val pendingCommonLoginStore: PendingCommonLoginStore
) {
    fun isEnabled(workspace: WorkspaceKey): Boolean {
        return sessionProvider.hasValidSession(workspace) || pendingCommonLoginStore.canEnter(workspace)
    }

    suspend fun navigate(
        workspace: WorkspaceKey,
        navController: NavController
    ): AppEntryNavigationResult {
        if (!isEnabled(workspace)) {
            return AppEntryNavigationResult.Blocked
        }

        if (!sessionProvider.hasValidSession(workspace)) {
            val pendingMicrosoftAuth = pendingCommonLoginStore.pendingMicrosoftAuth()
                ?: return AppEntryNavigationResult.Blocked

            val result = authSessionRepositoryProvider.repositoryFor(workspace)
                .signInWithMicrosoftToken(
                    idToken = pendingMicrosoftAuth.idToken,
                    graphAccessToken = pendingMicrosoftAuth.graphAccessToken
                )

            if (!result.isSuccess) {
                return AppEntryNavigationResult.Error(result.errorMessage ?: "Login failed.")
            }

            pendingCommonLoginStore.clear()
        }

        val action = when (workspace) {
            WorkspaceKey.HRIS -> R.id.action_appEntryFragment_to_homeFragment

            WorkspaceKey.INTERNAL_APPS -> R.id.action_appEntryFragment_to_iaInternalAppsNavGraph
        }

        val destinationName = when (workspace) {
            WorkspaceKey.HRIS -> "HRIS"
            WorkspaceKey.INTERNAL_APPS -> "Internal Apps"
        }

        try {
            navController.navigate(
                action,
                bundleOf(),
                NavOptions.Builder()
                    .setPopUpTo(R.id.appEntryFragment, true)
                    .build()
            )
        } catch (exception: IllegalArgumentException) {
            return AppEntryNavigationResult.Error("$destinationName navigation is not available in this build.")
        }
        return AppEntryNavigationResult.Success
    }
}

sealed interface AppEntryNavigationResult {
    data object Success : AppEntryNavigationResult
    data object Blocked : AppEntryNavigationResult
    data class Error(val message: String) : AppEntryNavigationResult
}
