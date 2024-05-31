package com.axelliant.hrms.model.login

import com.axelliant.hrms.model.base.Meta

data class CheckInResponse(
    val access_token: String?=null,
    val refresh_token: String?=null,
    val meta: Meta
)
