package com.axelliant.hris.core.contracts.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey

interface WorkspaceSessionProvider {
    fun canEnterWorkspace(workspace: WorkspaceKey): Boolean
    fun hasValidSession(workspace: WorkspaceKey): Boolean
    fun currentSession(workspace: WorkspaceKey): AppSession?
    fun clearSession(workspace: WorkspaceKey)
    fun clearAllSessions()
}
