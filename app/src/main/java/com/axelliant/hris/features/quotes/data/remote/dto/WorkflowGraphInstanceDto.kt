package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkflowGraphInstanceDto(
    @SerializedName("instanceId")
    val instanceId: String? = null,

    @SerializedName("state")
    val state: String? = null,

    @SerializedName("nodes")
    val nodes: List<WorkflowGraphNodeDto>? = null
)

data class WorkflowGraphNodeDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("workflowGraphInstanceId")
    val workflowGraphInstanceId: String? = null,

    @SerializedName("nodeId")
    val nodeId: String? = null,

    @SerializedName("processName")
    val processName: String? = null,

    @SerializedName("nodeType")
    val nodeType: String? = null,

    @SerializedName("roleIds")
    val roleIds: List<String>? = null,

    @SerializedName("state")
    val state: String? = null,

    @SerializedName("completedBy")
    val completedBy: String? = null,

    @SerializedName("completedOn")
    val completedOn: Long? = null,

    @SerializedName("comments")
    val comments: String? = null,

    @SerializedName("sequence")
    val sequence: Int? = null
)