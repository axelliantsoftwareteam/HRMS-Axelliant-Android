package com.axelliant.hris.features.auth.data

import android.util.Base64
import android.util.Log
import com.axelliant.hris.BuildConfig
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.core.session.UserSession
import com.axelliant.hris.core.session.toUserSessionFromAccessToken
import com.axelliant.hris.features.auth.data.remote.AuthApiService
import com.axelliant.hris.features.auth.data.remote.dto.LoginRequest
import com.axelliant.hris.features.auth.data.remote.dto.LoginResponse
import com.axelliant.hris.features.auth.data.remote.dto.MicrosoftTokenData
import com.axelliant.hris.features.auth.data.remote.dto.MicrosoftTokenRequest
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val result = safeApiExecutor.execute {
            apiService.login(LoginRequest(username = username, password = password))
        }
        if (result is ApiResult.Success) {
            val session = result.data.toUserSessionOrNull()
                ?: return ApiResult.UnknownError(result.data.loginErrorMessage())
            sessionManager.saveSession(session)
        }
        return result
    }

    suspend fun loginWithMicrosoft(
        idToken: String,
        graphAccessToken: String? = null
    ): ApiResult<BaseApiModel<MicrosoftTokenData>> {
        Log.i(TAG, "POST ${BuildConfig.API_BASE_URL}auth/GetToken (tokens omitted)")
        val result = safeApiExecutor.execute {
            apiService.getMicrosoftToken(
                MicrosoftTokenRequest(
                    idToken = idToken,
                    graphAccessToken = graphAccessToken.orEmpty()
                )
            )
        }
        Log.i(TAG, "auth/GetToken result=${result.toAuthLogName()}")
        if (result is ApiResult.Success) {
            val tokenData = result.data.data?.data
            val accessToken = tokenData?.resolvedAccessToken()
            if (!accessToken.isNullOrBlank()) {
                sessionManager.saveSession(accessToken.toMicrosoftUserSession())
                graphAccessToken?.takeIf { it.isNotBlank() }
                    ?.let(sessionManager::saveMicrosoftGraphToken)
            } else {
                return ApiResult.UnknownError(result.data.microsoftLoginErrorMessage())
            }
        }
        return result
    }

    suspend fun logout(): ApiResult<Unit> {
        val result = safeApiExecutor.execute { apiService.logout() }
        sessionManager.clearSession()
        return result
    }

    private fun LoginResponse.toUserSessionOrNull(): UserSession? {
        val resolvedAccessToken = data?.data?.accessToken?.takeIf { it.isNotBlank() }
            ?: accessToken?.takeIf { it.isNotBlank() }
            ?: return null
        val tokenSession = resolvedAccessToken.toUserSessionFromAccessToken()
        return UserSession(
            accessToken = resolvedAccessToken,
            refreshToken = refreshToken,
            userId = id?.toString()?.takeIf { it.isNotBlank() } ?: tokenSession.userId,
            displayName = listOfNotNull(firstName, lastName)
                .joinToString(" ")
                .ifBlank { username.orEmpty() }
                .ifBlank { tokenSession.displayName.orEmpty() },
            email = email?.takeIf { it.isNotBlank() } ?: tokenSession.email
        )
    }

    private fun LoginResponse.loginErrorMessage(): String {
        return data?.message
            ?: message?.text
            ?: "Login failed. Access token was missing from the response."
    }

    private fun BaseApiModel<MicrosoftTokenData>.microsoftLoginErrorMessage(): String {
        return data?.message
            ?: message?.text
            ?: "Microsoft login failed. Internal Apps token was missing from the response."
    }

    private fun MicrosoftTokenData.resolvedAccessToken(): String? {
        return accessToken?.takeIf { it.isNotBlank() }
            ?: aeAccessToken?.takeIf { it.isNotBlank() }
    }

    private fun String.toMicrosoftUserSession(): UserSession {
        val claims = decodeJwtClaims()
        return UserSession(
            accessToken = this,
            refreshToken = null,
            userId = claims?.optString(CLAIM_USER_ID).orEmpty(),
            displayName = claims?.optString(CLAIM_DISPLAY_NAME).orEmpty(),
            email = claims?.optString(CLAIM_EMAIL).orEmpty()
        )
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

    private fun ApiResult<*>.toAuthLogName(): String {
        return when (this) {
            is ApiResult.Success -> "success"
            ApiResult.Empty -> "empty"
            is ApiResult.HttpError -> "http_error_$code"
            is ApiResult.NetworkError -> "network_error"
            is ApiResult.UnknownError -> "unknown_error"
            ApiResult.Unauthorized -> "unauthorized"
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val JWT_PAYLOAD_INDEX = 1
        const val CLAIM_EMAIL =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
        const val CLAIM_USER_ID = "userId"
        const val CLAIM_DISPLAY_NAME = "displayName"
    }
}
