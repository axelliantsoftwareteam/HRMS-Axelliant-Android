package com.axelliant.hris.core.contracts.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionContractAdapter @Inject constructor(
    private val workspaceSessionProvider: WorkspaceSessionProvider
) : SessionContract {
    override fun hasValidSession(): Boolean {
        return workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)
    }

    override fun currentSession(): AppSession? {
        return workspaceSessionProvider.currentSession(WorkspaceKey.HRIS)
    }

    override fun canAccess(workspace: WorkspaceKey): Boolean {
        return workspaceSessionProvider.hasValidSession(workspace)
    }

    override fun clearSession() {
        workspaceSessionProvider.clearAllSessions()
    }
}
