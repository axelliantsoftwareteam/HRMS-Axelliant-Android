package com.axelliant.hris.core.auth

import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthManager
import com.axelliant.hris.features.auth.microsoft.MicrosoftSignOutResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalLogoutCoordinator @Inject constructor(
    private val workspaceSessionProvider: WorkspaceSessionProvider,
    private val pendingCommonLoginStore: PendingCommonLoginStore,
    private val microsoftAuthManager: MicrosoftAuthManager
) {
    suspend fun logout(): GlobalLogoutResult {
        workspaceSessionProvider.clearAllSessions()
        pendingCommonLoginStore.clear()

        return when (val microsoftResult = microsoftAuthManager.signOut()) {
            MicrosoftSignOutResult.Success -> GlobalLogoutResult.Success
            is MicrosoftSignOutResult.Error -> GlobalLogoutResult.CompletedWithMicrosoftError(
                microsoftResult.message.orEmpty().ifBlank { "Microsoft sign-out failed." }
            )
        }
    }
}

sealed interface GlobalLogoutResult {
    data object Success : GlobalLogoutResult
    data class CompletedWithMicrosoftError(val message: String) : GlobalLogoutResult
}
