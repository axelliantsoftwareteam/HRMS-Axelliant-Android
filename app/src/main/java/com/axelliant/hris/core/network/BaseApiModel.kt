package com.axelliant.hris.core.network

import com.google.gson.annotations.SerializedName

data class BaseApiModel<T>(
    @SerializedName("data")
    val data: BaseModel<T>? = null,
    @SerializedName("message")
    val message: ApiMessage? = null
)

data class BaseModel<T>(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null
)

data class ApiMessage(
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("messageTypeId")
    val messageTypeId: String? = null
)
