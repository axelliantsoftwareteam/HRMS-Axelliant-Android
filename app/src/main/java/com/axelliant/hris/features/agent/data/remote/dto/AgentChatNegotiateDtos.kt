package com.axelliant.hris.features.agent.data.remote.dto

data class AgentChatInitialNegotiateDto(
    val negotiateVersion: Int? = null,
    val url: String? = null,
    val accessToken: String? = null,
    val availableTransports: List<AgentChatTransportDto> = emptyList()
)

data class AgentChatConnectionNegotiateDto(
    val negotiateVersion: Int? = null,
    val connectionId: String? = null,
    val connectionToken: String? = null,
    val availableTransports: List<AgentChatTransportDto> = emptyList()
)

data class AgentChatTransportDto(
    val transport: String? = null,
    val transferFormats: List<String> = emptyList()
)
