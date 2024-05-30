package com.axelliant.hrms.model.login

import com.axelliant.hrms.model.base.Meta

data class UserLoginResponse(
    val access_token: String?=null,
    val refresh_token: String?=null,
    val meta: Meta
)
