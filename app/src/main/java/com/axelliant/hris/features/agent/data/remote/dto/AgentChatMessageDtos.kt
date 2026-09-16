package com.axelliant.hris.features.agent.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class AgentChatMessageRequestDto(
    val message: String,
    val context: AgentChatMessageContextDto,
    @SerializedName("run_id")
    val runId: String? = null,
    val resume: Boolean = false,
    val confirmation: Boolean = false
)

data class AgentChatMessageContextDto(
    val action: String,
    val search: String
)

data class AgentChatMessageResponseDto(
    @SerializedName("run_id")
    val runId: String? = null,
    val status: String? = null,
    val reply: String? = null,
    @SerializedName("requires_confirmation")
    val requiresConfirmation: Boolean = false,
    @SerializedName("created_quote_id")
    val createdQuoteId: String? = null,
    @SerializedName("quote_serial")
    val quoteSerial: String? = null,
    @SerializedName("used_tools")
    val usedTools: List<JsonElement> = emptyList(),
    @SerializedName("tool_results")
    val toolResults: List<JsonElement> = emptyList(),
    @SerializedName("progress_events")
    val progressEvents: List<JsonElement> = emptyList()
)
