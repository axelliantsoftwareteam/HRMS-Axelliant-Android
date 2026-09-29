package com.axelliant.hris.features.agent.data

import android.util.Log
import com.axelliant.hris.BuildConfig
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.agent.data.remote.AgentChatApiService
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatMessageContextDto
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatMessageRequestDto
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatMessageResponseDto
import javax.inject.Inject

class AgentChatRepository @Inject constructor(
    private val apiService: AgentChatApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val sessionManager: AgentChatSessionManager
) {
    fun getSavedSession(): AgentChatSession? = sessionManager.getSession()

    suspend fun sendMessage(message: String): ApiResult<AgentChatMessageResponseDto> {
        Log.d(TAG, "Preparing AgentChat/message. messageLength=${message.length}")
        val session = when (val sessionResult = ensureSession()) {
            is ApiResult.Success -> sessionResult.data
            ApiResult.Empty -> return ApiResult.UnknownError("Agent session response is empty.")
            is ApiResult.HttpError -> return sessionResult
            is ApiResult.NetworkError -> return sessionResult
            is ApiResult.UnknownError -> return sessionResult
            ApiResult.Unauthorized -> return ApiResult.Unauthorized
        }

        val request = AgentChatMessageRequestDto(
            message = message,
            context = AgentChatMessageContextDto(
                action = "search_products",
                search = message
            ),
            runId = session.runId,
            resume = false,
            confirmation = false
        )

        Log.d(TAG, "POST ${BuildConfig.API_BASE_URL}AgentChat/message")
        Log.d(TAG, "AgentChat/message payload: messageLength=${message.length}, action=search_products, search=$message, hasRunId=${!session.runId.isNullOrBlank()}, resume=false, confirmation=false")
        val messageResult = safeApiExecutor.execute {
            apiService.sendMessage(request)
        }

        if (messageResult is ApiResult.Success) {
            sessionManager.saveSession(session.copy(runId = messageResult.data.runId))
        }

        Log.d(TAG, "AgentChat/message result=${messageResult::class.java.simpleName}")
        return messageResult
    }

    suspend fun ensureSession(): ApiResult<AgentChatSession> {
        sessionManager.getSession()?.let { return ApiResult.Success(it) }

        Log.d(TAG, "POST ${BuildConfig.API_BASE_URL}agent-chat-hub/negotiate?negotiateVersion=1")
        val initialResult = safeApiExecutor.execute {
            apiService.negotiateAgentChatHub()
        }
        Log.d(TAG, "agent-chat-hub/negotiate result=${initialResult::class.java.simpleName}")

        val initialSession = when (initialResult) {
            is ApiResult.Success -> initialResult.data
            ApiResult.Empty -> return ApiResult.UnknownError("Agent session response is empty.")
            is ApiResult.HttpError -> return initialResult
            is ApiResult.NetworkError -> return initialResult
            is ApiResult.UnknownError -> return initialResult
            ApiResult.Unauthorized -> return ApiResult.Unauthorized
        }

        val negotiateUrl = initialSession.url.orEmpty().toSignalRNegotiateUrl()
        val accessToken = initialSession.accessToken.orEmpty()
        if (negotiateUrl.isBlank() || accessToken.isBlank()) {
            return ApiResult.UnknownError("Agent session response is missing connection details.")
        }

        Log.d(TAG, "POST $negotiateUrl")
        val connectionResult = safeApiExecutor.execute {
            apiService.negotiateSignalRClient(
                url = negotiateUrl,
                authorization = "Bearer $accessToken"
            )
        }
        Log.d(TAG, "SignalR client negotiate result=${connectionResult::class.java.simpleName}")

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

    private fun String.toSignalRNegotiateUrl(): String {
        return replace("/client/?", "/client/negotiate?")
    }

    private companion object {
        const val TAG = "AgentChatRepository"
    }
}
