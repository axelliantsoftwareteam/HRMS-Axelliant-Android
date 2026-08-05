package com.axelliant.hris.core.contracts.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.utils.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrisSessionContractAdapter @Inject constructor(
    private val sessionManager: SessionManager
) : SessionContract {
    override fun hasValidSession(): Boolean {
        return sessionManager.checkLogin()
    }

    override fun currentSession(): AppSession? {
        if (!hasValidSession()) return null

        return AppSession(
            email = sessionManager.getUserEmail(),
            displayName = sessionManager.getFirstName(),
            accessToken = sessionManager.getToken()
        )
    }

    override fun canAccess(workspace: WorkspaceKey): Boolean {
        return workspace == WorkspaceKey.HRIS && hasValidSession()
    }

    override fun clearSession() {
        sessionManager.logoutUser()
    }
}
