package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkflowProcessDto(
    @SerializedName("serialId")
    val serialId: String? = null,
    @SerializedName("processNo")
    val processNo: Int? = null,
    @SerializedName("roleId")
    val roleId: String? = null,
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("processName")
    val processName: String? = null,
    @SerializedName("processScreenName")
    val processScreenName: String? = null,
    @SerializedName("processStatus")
    val processStatus: Boolean? = null,
    @SerializedName("status")
    val status: Boolean? = null,
    @SerializedName("comments")
    val comments: String? = null,
    @SerializedName("isCompletelyRejected")
    val isCompletelyRejected: Boolean? = null,
    @SerializedName("timeStamp")
    val timeStamp: Long? = null,
    @SerializedName("relationType")
    val relationType: String? = null,
    @SerializedName("relationId")
    val relationId: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("approveText")
    val approveText: String? = null,
    @SerializedName("rejectText")
    val rejectText: String? = null,
    @SerializedName("roleName")
    val roleName: String? = null,
    @SerializedName("timestampDate")
    val timestampDate: String? = null
)
