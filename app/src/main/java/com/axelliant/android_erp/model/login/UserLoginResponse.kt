package com.axelliant.android_erp.model.login

import com.axelliant.android_erp.model.base.Meta

data class UserLoginResponse(
    val access_token: String?=null,
    val refresh_token: String?=null,
    val meta: Meta
)
