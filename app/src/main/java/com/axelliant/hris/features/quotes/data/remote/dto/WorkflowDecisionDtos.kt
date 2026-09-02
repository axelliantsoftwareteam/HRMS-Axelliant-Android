package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkflowDecisionRequest(
    @SerializedName("instanceId")
    val instanceId: String,

    @SerializedName("nodeId")
    val nodeId: String,

    @SerializedName("approved")
    val approved: Boolean,

    @SerializedName("comments")
    val comments: String,

    @SerializedName("idempotencyKey")
    val idempotencyKey: String,

    @SerializedName("data")
    val data: Map<String, Any> = emptyMap()
)
