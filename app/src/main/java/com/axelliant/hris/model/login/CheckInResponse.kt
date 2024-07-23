package com.axelliant.hris.model.login

import com.axelliant.hris.model.base.Meta

data class CheckInResponse(
    val status_message: String?=null,
    val meta: Meta
)
