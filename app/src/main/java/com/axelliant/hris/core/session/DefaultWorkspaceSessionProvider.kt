package com.axelliant.hris.core.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.session.SessionManager as InternalAppsSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWorkspaceSessionProvider @Inject constructor(
    private val hrisSessionStore: HrisSessionStore,
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
            WorkspaceKey.HRIS -> hrisSessionStore.hasValidSession()
            WorkspaceKey.INTERNAL_APPS -> internalAppsSessionManager.isLoggedIn()
        }
    }

    override fun currentSession(workspace: WorkspaceKey): AppSession? {
        if (!hasValidSession(workspace)) return null

        return when (workspace) {
            WorkspaceKey.HRIS -> hrisSessionStore.currentSession()

            WorkspaceKey.INTERNAL_APPS -> AppSession(
                accessToken = internalAppsSessionManager.getAccessToken()
            )
        }
    }

    override fun clearSession(workspace: WorkspaceKey) {
        when (workspace) {
            WorkspaceKey.HRIS -> hrisSessionStore.clearSession()
            WorkspaceKey.INTERNAL_APPS -> internalAppsSessionManager.clearSession()
        }
    }

    override fun clearAllSessions() {
        hrisSessionStore.clearSession()
        internalAppsSessionManager.clearSession()
    }
}
