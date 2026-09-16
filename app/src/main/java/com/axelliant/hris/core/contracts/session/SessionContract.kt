package com.axelliant.hris.core.contracts.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey

interface SessionContract {
    fun hasValidSession(): Boolean
    fun currentSession(): AppSession?
    fun canAccess(workspace: WorkspaceKey): Boolean
    fun clearSession()
}
