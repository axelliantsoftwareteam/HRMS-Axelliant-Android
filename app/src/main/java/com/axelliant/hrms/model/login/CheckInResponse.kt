package com.axelliant.hrms.model.login

import com.axelliant.hrms.model.base.Meta

data class CheckInResponse(
    val status_message: String?=null,
    val meta: Meta
)
