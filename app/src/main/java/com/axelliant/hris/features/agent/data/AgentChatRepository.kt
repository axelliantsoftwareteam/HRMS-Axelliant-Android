package com.axelliant.hris.features.agent.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.agent.data.remote.AgentChatApiService
import javax.inject.Inject

class AgentChatRepository @Inject constructor(
    private val apiService: AgentChatApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val sessionManager: AgentChatSessionManager
) {
    fun getSavedSession(): AgentChatSession? = sessionManager.getSession()

    suspend fun ensureSession(): ApiResult<AgentChatSession> {
        sessionManager.getSession()?.let { return ApiResult.Success(it) }

        val initialResult = safeApiExecutor.execute {
            apiService.negotiateAgentChatHub()
        }

        val initialSession = when (initialResult) {
            is ApiResult.Success -> initialResult.data
            ApiResult.Empty -> return ApiResult.UnknownError("Agent session response is empty.")
            is ApiResult.HttpError -> return initialResult
            is ApiResult.NetworkError -> return initialResult
            is ApiResult.UnknownError -> return initialResult
            ApiResult.Unauthorized -> return ApiResult.Unauthorized
        }

        val negotiateUrl = initialSession.url.orEmpty()
        val accessToken = initialSession.accessToken.orEmpty()
        if (negotiateUrl.isBlank() || accessToken.isBlank()) {
            return ApiResult.UnknownError("Agent session response is missing connection details.")
        }

        val connectionResult = safeApiExecutor.execute {
            apiService.negotiateSignalRClient(
                url = negotiateUrl,
                authorization = "Bearer $accessToken"
            )
        }

        val connection = when (connectionResult) {
            is ApiResult.Success -> connectionResult.data
            ApiResult.Empty -> return ApiResult.UnknownError("Agent connection response is empty.")
            is ApiResult.HttpError -> return connectionResult
            is ApiResult.NetworkError -> return connectionResult
            is ApiResult.UnknownError -> return connectionResult
            ApiResult.Unauthorized -> return ApiResult.Unauthorized
        }

        val session = AgentChatSession(
            negotiateUrl = negotiateUrl,
            accessToken = accessToken,
            connectionId = connection.connectionId.orEmpty(),
            connectionToken = connection.connectionToken.orEmpty()
        )

        if (session.connectionId.isBlank() || session.connectionToken.isBlank()) {
            return ApiResult.UnknownError("Agent connection response is missing session details.")
        }

        sessionManager.saveSession(session)
        return ApiResult.Success(session)
    }

    fun clearSession() {
        sessionManager.clearSession()
    }
}
