package com.axelliant.hris.features.agent.data.remote

import com.axelliant.hris.features.agent.data.remote.dto.AgentChatConnectionNegotiateDto
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatInitialNegotiateDto
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatMessageRequestDto
import com.axelliant.hris.features.agent.data.remote.dto.AgentChatMessageResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AgentChatApiService {
    @POST("agent-chat-hub/negotiate?negotiateVersion=1")
    suspend fun negotiateAgentChatHub(): Response<AgentChatInitialNegotiateDto>
    @POST
    suspend fun negotiateSignalRClient(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Response<AgentChatConnectionNegotiateDto>

    @POST("AgentChat/message")
    suspend fun sendMessage(
        @Body request: AgentChatMessageRequestDto
    ): Response<AgentChatMessageResponseDto>
}
