package com.axelliant.hris.core.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.session.SessionManager as InternalAppsSessionManager
import com.axelliant.hris.utils.SessionManager as HrisSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWorkspaceSessionProvider @Inject constructor(
    private val hrisSessionManager: HrisSessionManager,
    private val internalAppsSessionManager: InternalAppsSessionManager
) : WorkspaceSessionProvider {

    override fun canEnterWorkspace(workspace: WorkspaceKey): Boolean {
        return when (workspace) {
            WorkspaceKey.HRIS,
            WorkspaceKey.INTERNAL_APPS -> true
        }
    }

    override fun hasValidSession(workspace: WorkspaceKey): Boolean {
        return when (workspace) {
            WorkspaceKey.HRIS -> hrisSessionManager.checkLogin()
            WorkspaceKey.INTERNAL_APPS -> internalAppsSessionManager.isLoggedIn()
        }
    }

    override fun currentSession(workspace: WorkspaceKey): AppSession? {
        if (!hasValidSession(workspace)) return null

        return when (workspace) {
            WorkspaceKey.HRIS -> AppSession(
                email = hrisSessionManager.getUserEmail(),
                displayName = hrisSessionManager.getFirstName(),
                accessToken = hrisSessionManager.getToken()
            )

            WorkspaceKey.INTERNAL_APPS -> AppSession(
                accessToken = internalAppsSessionManager.getAccessToken()
            )
        }
    }

    override fun clearSession(workspace: WorkspaceKey) {
        when (workspace) {
            WorkspaceKey.HRIS -> hrisSessionManager.logoutUser()
            WorkspaceKey.INTERNAL_APPS -> internalAppsSessionManager.clearSession()
        }
    }

    override fun clearAllSessions() {
        hrisSessionManager.logoutUser()
        internalAppsSessionManager.clearSession()
    }
}
