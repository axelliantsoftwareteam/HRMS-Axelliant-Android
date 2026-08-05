package com.axelliant.hris.core.contracts.navigation

interface AppNavigationContract {
    fun openWorkspace(workspace: WorkspaceKey)
    fun openLogin(workspace: WorkspaceKey)
    fun openHome(workspace: WorkspaceKey)
    fun goBack(): Boolean
}
