package com.axelliant.hris.core.auth

import com.axelliant.hris.core.contracts.auth.AuthSessionRepository
import com.axelliant.hris.core.contracts.auth.WorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWorkspaceAuthSessionRepositoryProvider @Inject constructor(
    private val hrisAuthSessionRepository: HrisAuthSessionRepository,
    private val internalAppsAuthSessionRepository: InternalAppsAuthSessionRepository
) : WorkspaceAuthSessionRepositoryProvider {

    override fun repositoryFor(workspace: WorkspaceKey): AuthSessionRepository {
        return when (workspace) {
            WorkspaceKey.HRIS -> hrisAuthSessionRepository
            WorkspaceKey.INTERNAL_APPS -> internalAppsAuthSessionRepository
        }
    }
}
