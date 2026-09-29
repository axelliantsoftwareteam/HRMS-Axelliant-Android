package com.axelliant.hris.core.auth

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import javax.inject.Inject
import javax.inject.Singleton

enum class PendingCommonLoginMethod {
    NONE,
    INTERNAL_APPS_PASSWORD,
    MICROSOFT
}

data class PendingMicrosoftAuth(
    val idToken: String,
    val graphAccessToken: String?
)

@Singleton
class PendingCommonLoginStore @Inject constructor() {
    private var method: PendingCommonLoginMethod = PendingCommonLoginMethod.NONE
    private var allowedWorkspaces: Set<WorkspaceKey> = emptySet()
    private var microsoftAuth: PendingMicrosoftAuth? = null

    fun markInternalAppsPasswordAuthenticated() {
        method = PendingCommonLoginMethod.INTERNAL_APPS_PASSWORD
        allowedWorkspaces = setOf(WorkspaceKey.INTERNAL_APPS)
        microsoftAuth = null
    }

    fun markMicrosoftAuthenticated(idToken: String, graphAccessToken: String?) {
        method = PendingCommonLoginMethod.MICROSOFT
        allowedWorkspaces = setOf(WorkspaceKey.HRIS, WorkspaceKey.INTERNAL_APPS)
        microsoftAuth = PendingMicrosoftAuth(idToken, graphAccessToken)
    }

    fun markExistingSessionAuthenticated(workspaces: Set<WorkspaceKey>) {
        method = PendingCommonLoginMethod.NONE
        allowedWorkspaces = workspaces
        microsoftAuth = null
    }

    fun canEnter(workspace: WorkspaceKey): Boolean = workspace in allowedWorkspaces

    fun currentMethod(): PendingCommonLoginMethod = method

    fun pendingMicrosoftAuth(): PendingMicrosoftAuth? = microsoftAuth

    fun markMicrosoftWorkspacePreparationCompleted(workspaces: Set<WorkspaceKey>) {
        method = PendingCommonLoginMethod.NONE
        allowedWorkspaces = workspaces
        microsoftAuth = null
    }

    fun clear() {
        method = PendingCommonLoginMethod.NONE
        allowedWorkspaces = emptySet()
        microsoftAuth = null
    }
}
