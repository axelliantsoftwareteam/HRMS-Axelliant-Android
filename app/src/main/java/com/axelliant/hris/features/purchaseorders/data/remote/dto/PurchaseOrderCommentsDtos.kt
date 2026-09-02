package com.axelliant.hris.features.purchaseorders.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PurchaseOrderCommentDto(
    @SerializedName("comment")
    val comment: String? = null,

    @SerializedName("commentedBy")
    val commentedBy: String? = null,

    @SerializedName("commentedDate")
    val commentedDate: String? = null,

    @SerializedName("roleName")
    val roleName: String? = null,

    @SerializedName("processName")
    val processName: String? = null,

    @SerializedName("processNo")
    val processNo: Int? = null,

    @SerializedName("commentedOn")
    val commentedOn: Long? = null,

    @SerializedName("status")
    val status: Boolean? = null
)
