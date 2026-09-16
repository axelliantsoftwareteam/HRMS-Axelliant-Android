package com.axelliant.hris.core.auth

import android.util.Base64
import android.util.Log
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
import org.json.JSONObject
import java.nio.charset.StandardCharsets

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
        logMicrosoftTokenDiagnostics(idToken)
        return suspendCancellableCoroutine { continuation ->
            Log.i(TAG, "POST https://hris.axelliant.com/api/method/hrms.api.mobile_v1.get_set_user_token (microsoft_token omitted)")
            val liveData = loginRepo.userLoginApiCall(LoginRequest(microsoft_token = idToken))
            val observer = object : Observer<BaseApiModel<UserLoginResponse>?> {
                override fun onChanged(value: BaseApiModel<UserLoginResponse>?) {
                    liveData.removeObserver(this)
                    if (!continuation.isActive) return

                    val response = value?.message?.data
                    if (response?.meta?.status == true) {
                        Log.i(TAG, "HRIS Microsoft login result=success")
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
                        Log.w(TAG, "HRIS Microsoft login result=failure message=${response?.meta?.message ?: "HRIS login failed."}")
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

    private fun logMicrosoftTokenDiagnostics(idToken: String) {
        val claims = idToken.decodeJwtClaims()
        if (claims == null) {
            Log.w(TAG, "Microsoft idToken diagnostics: tokenFormat=unreadable_jwt")
            return
        }
        val claimSummary = listOf(
            "aud=${claims.optString("aud").maskLongClaim()}",
            "iss=${claims.optString("iss").maskLongClaim()}",
            "tid=${claims.optString("tid")}",
            "oid=${claims.optString("oid")}",
            "sub=${claims.optString("sub").maskLongClaim()}",
            "preferred_username=${claims.optString("preferred_username")}",
            "email=${claims.optString("email").ifBlank { claims.optString(CLAIM_EMAIL) }}",
            "upn=${claims.optString("upn")}",
            "name=${claims.optString("name")}",
            "exp=${claims.optLong("exp", 0L)}"
        ).joinToString(separator = ", ")
        Log.i(TAG, "Microsoft idToken diagnostics: $claimSummary")
    }

    private fun String.decodeJwtClaims(): JSONObject? {
        return runCatching {
            val payload = split(".").getOrNull(JWT_PAYLOAD_INDEX).orEmpty()
            val decodedBytes = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
        }.getOrNull()
    }

    private fun String.maskLongClaim(): String {
        return when {
            isBlank() -> ""
            length <= CLAIM_VISIBLE_PREFIX -> this
            else -> take(CLAIM_VISIBLE_PREFIX) + "..."
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

    private companion object {
        const val TAG = "HrisAuthSessionRepository"
        const val JWT_PAYLOAD_INDEX = 1
        const val CLAIM_VISIBLE_PREFIX = 12
        const val CLAIM_EMAIL =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
    }
}
