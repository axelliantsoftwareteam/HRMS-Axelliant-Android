package com.axelliant.hris.core.auth

import androidx.lifecycle.Observer
import com.axelliant.hris.core.contracts.auth.AuthSessionRepository
import com.axelliant.hris.core.contracts.auth.AuthSessionResult
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.core.session.HrisSessionStore
import com.axelliant.hris.model.base.BaseApiModel
import com.axelliant.hris.model.login.LoginRequest
import com.axelliant.hris.model.login.UserLoginResponse
import com.axelliant.hris.repos.LoginRepo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class HrisAuthSessionRepository @Inject constructor(
    private val loginRepo: LoginRepo,
    private val hrisSessionStore: HrisSessionStore,
    private val hrisTokenProvider: HrisTokenProvider
) : AuthSessionRepository {
    override val workspace: WorkspaceKey = WorkspaceKey.HRIS

    override fun hasValidSession(): Boolean {
        return hrisSessionStore.hasValidSession()
    }

    override fun currentSession(): AppSession? {
        return hrisSessionStore.currentSession()
    }

    override suspend fun signInWithPassword(
        username: String,
        password: String
    ): AuthSessionResult {
        return AuthSessionResult(
            errorMessage = "Password sign-in is not available for the current HRIS login flow."
        )
    }

    override suspend fun signInWithMicrosoftToken(
        idToken: String,
        graphAccessToken: String?
    ): AuthSessionResult {
        return suspendCancellableCoroutine { continuation ->
            val liveData = loginRepo.userLoginApiCall(LoginRequest(microsoft_token = idToken))
            val observer = object : Observer<BaseApiModel<UserLoginResponse>?> {
                override fun onChanged(value: BaseApiModel<UserLoginResponse>?) {
                    liveData.removeObserver(this)
                    if (!continuation.isActive) return

                    val response = value?.message?.data
                    if (response?.meta?.status == true) {
                        val token = response.toHrisAccessToken()
                        if (token.isNullOrBlank()) {
                            continuation.resume(
                                AuthSessionResult(errorMessage = "HRIS login token was missing.")
                            )
                            return
                        }
                        saveSession(response, token)
                        continuation.resume(AuthSessionResult(session = currentSession()))
                    } else {
                        continuation.resume(
                            AuthSessionResult(
                                errorMessage = response?.meta?.message ?: "HRIS login failed."
                            )
                        )
                    }
                }
            }

            liveData.observeForever(observer)
            continuation.invokeOnCancellation { liveData.removeObserver(observer) }
        }
    }

    override suspend fun signOut(): AuthSessionResult {
        clearSession()
        return AuthSessionResult()
    }

    override fun clearSession() {
        hrisSessionStore.clearSession()
    }

    private fun saveSession(response: UserLoginResponse, token: String) {
        val email = response.access_token?.email
        hrisSessionStore.saveMicrosoftSession(email, token)
        hrisTokenProvider.saveToken(token)
    }

    private fun UserLoginResponse.toHrisAccessToken(): String? {
        val apiKey = access_token?.api_key?.takeIf { it.isNotBlank() }
        val apiSecret = access_token?.api_sec?.takeIf { it.isNotBlank() }
            ?: access_token?.api_secret?.takeIf { it.isNotBlank() }
        return if (apiKey != null && apiSecret != null) {
            "$apiKey:$apiSecret"
        } else {
            token?.takeIf { it.isNotBlank() }
        }
    }
}
