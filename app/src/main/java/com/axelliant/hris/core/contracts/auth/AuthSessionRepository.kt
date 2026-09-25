package com.axelliant.hris.core.contracts.auth

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession

data class AuthSessionResult(
    val session: AppSession? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = session != null && errorMessage == null
}

interface AuthSessionRepository {
    val workspace: WorkspaceKey

    fun hasValidSession(): Boolean
    fun currentSession(): AppSession?
    suspend fun signInWithPassword(username: String, password: String): AuthSessionResult
    suspend fun signInWithMicrosoftToken(
        idToken: String,
        graphAccessToken: String? = null
    ): AuthSessionResult
    suspend fun signOut(): AuthSessionResult
    fun clearSession()
}

interface WorkspaceAuthSessionRepositoryProvider {
    fun repositoryFor(workspace: WorkspaceKey): AuthSessionRepository
}
