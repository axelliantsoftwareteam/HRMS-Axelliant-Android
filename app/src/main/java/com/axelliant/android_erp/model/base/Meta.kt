package com.axelliant.android_erp.model.base

data class Meta(
    val message: String,
    val status: Boolean?=false,
    val status_code: Int?=501,
)