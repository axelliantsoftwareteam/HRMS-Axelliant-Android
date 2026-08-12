package com.axelliant.hris.features.agent.data

data class AgentChatSession(
    val negotiateUrl: String,
    val accessToken: String,
    val connectionId: String,
    val connectionToken: String,
    val runId: String? = null
)
