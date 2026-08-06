package com.axelliant.hris.core.auth

import com.axelliant.hris.core.contracts.auth.AuthSessionRepository
import com.axelliant.hris.core.contracts.auth.AuthSessionResult
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.core.network.ApiErrorMessages
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.features.auth.data.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternalAppsAuthSessionRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : AuthSessionRepository {
    override val workspace: WorkspaceKey = WorkspaceKey.INTERNAL_APPS

    override fun hasValidSession(): Boolean {
        return sessionManager.isLoggedIn()
    }

    override fun currentSession(): AppSession? {
        val accessToken = sessionManager.getAccessToken()?.takeIf { it.isNotBlank() }
            ?: return null
        return AppSession(accessToken = accessToken)
    }

    override suspend fun signInWithPassword(
        username: String,
        password: String
    ): AuthSessionResult {
        return authRepository.login(username, password).toAuthSessionResult()
    }

    override suspend fun signInWithMicrosoftToken(
        idToken: String,
        graphAccessToken: String?
    ): AuthSessionResult {
        return authRepository.loginWithMicrosoft(
            idToken = idToken,
            graphAccessToken = graphAccessToken
        ).toAuthSessionResult()
    }

    override suspend fun signOut(): AuthSessionResult {
        val result = authRepository.logout()
        return when (result) {
            is ApiResult.HttpError,
            is ApiResult.NetworkError,
            is ApiResult.UnknownError -> AuthSessionResult(errorMessage = result.message())

            ApiResult.Empty,
            is ApiResult.Success,
            ApiResult.Unauthorized -> AuthSessionResult()
        }
    }

    override fun clearSession() {
        sessionManager.clearSession()
    }

    private fun ApiResult<*>.toAuthSessionResult(): AuthSessionResult {
        return when (this) {
            is ApiResult.Success -> currentSession()?.let { AuthSessionResult(session = it) }
                ?: AuthSessionResult(errorMessage = "Internal Apps session was not saved.")

            ApiResult.Empty -> AuthSessionResult(errorMessage = ApiErrorMessages.EMPTY_BODY)
            is ApiResult.HttpError,
            is ApiResult.NetworkError,
            is ApiResult.UnknownError -> AuthSessionResult(errorMessage = message())

            ApiResult.Unauthorized -> AuthSessionResult(errorMessage = ApiErrorMessages.UNAUTHORIZED)
        }
    }

    private fun ApiResult<*>.message(): String {
        return when (this) {
            is ApiResult.HttpError -> message
            is ApiResult.NetworkError -> message
            is ApiResult.UnknownError -> message
            ApiResult.Empty -> ApiErrorMessages.EMPTY_BODY
            is ApiResult.Success -> ""
            ApiResult.Unauthorized -> ApiErrorMessages.UNAUTHORIZED
        }
    }
}
