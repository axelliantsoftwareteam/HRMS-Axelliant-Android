package com.axelliant.hris.features.profiles.data

import android.util.Base64
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthManager
import com.axelliant.hris.features.profiles.data.local.ProfileCacheStore
import com.axelliant.hris.features.profiles.data.remote.ProfilesApiService
import com.axelliant.hris.features.profiles.data.remote.dto.MicrosoftProfileResponse
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilesRepository @Inject constructor(
    private val apiService: ProfilesApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val microsoftAuthManager: MicrosoftAuthManager,
    private val sessionManager: SessionManager,
    private val profileCacheStore: ProfileCacheStore
) {
    suspend fun getCurrentProfile(): ApiResult<MicrosoftProfileResponse> {
        val graphToken = getCachedGraphToken() ?: refreshGraphToken()
            ?: return getCachedProfileOrError()
        val result = getProfileWithToken(graphToken)
        if (result is ApiResult.Success) {
            profileCacheStore.save(result.data)
            return result
        }
        if (result !is ApiResult.Unauthorized) {
            return getCachedProfileOrResult(result)
        }

        sessionManager.clearMicrosoftGraphToken()
        val refreshedToken = refreshGraphToken()
            ?: return getCachedProfileOrError()
        val retryResult = getProfileWithToken(refreshedToken)
        if (retryResult is ApiResult.Success) {
            profileCacheStore.save(retryResult.data)
            return retryResult
        }
        return getCachedProfileOrResult(retryResult)
    }

    private suspend fun getProfileWithToken(
        graphToken: String
    ): ApiResult<MicrosoftProfileResponse> {
        return safeApiExecutor.execute(handleUnauthorized = false) {
            apiService.getCurrentMicrosoftProfile(authorization = "Bearer $graphToken")
        }
    }

    private fun getCachedGraphToken(): String? {
        return sessionManager.getMicrosoftGraphToken()
            ?.takeUnless { it.isExpiredJwt() }
    }

    private suspend fun refreshGraphToken(): String? {
        return microsoftAuthManager.acquireGraphAccessToken()
            ?.takeUnless { it.isExpiredJwt() }
            ?.also(sessionManager::saveMicrosoftGraphToken)
    }

    private fun getCachedProfileOrError(): ApiResult<MicrosoftProfileResponse> {
        return profileCacheStore.get()?.let { ApiResult.Success(it) }
            ?: ApiResult.UnknownError(
                "Unable to load Microsoft profile. Please sign in again to refresh Microsoft access."
            )
    }

    private fun getCachedProfileOrResult(
        result: ApiResult<MicrosoftProfileResponse>
    ): ApiResult<MicrosoftProfileResponse> {
        return profileCacheStore.get()?.let { ApiResult.Success(it) } ?: result
    }

    private fun String.isExpiredJwt(): Boolean {
        val claims = decodeJwtClaims() ?: return false
        val expiresAt = claims.optLong(CLAIM_EXPIRATION, 0L)
        if (expiresAt <= 0L) return false
        val nowSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND
        return expiresAt <= nowSeconds + TOKEN_EXPIRY_SKEW_SECONDS
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

    private companion object {
        const val JWT_PAYLOAD_INDEX = 1
        const val CLAIM_EXPIRATION = "exp"
        const val MILLIS_PER_SECOND = 1000L
        const val TOKEN_EXPIRY_SKEW_SECONDS = 60L
    }
}
