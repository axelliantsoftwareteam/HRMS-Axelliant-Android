package com.axelliant.hris.model.base

import com.google.gson.annotations.SerializedName

// BaseApiModel class
data class BaseApiModel<T>(
    @SerializedName("message")
    val message: BaseModel<T>?,

    )

// BaseModel class
data class BaseModel<T>(
    @SerializedName("data")
    val data: T?,

)




