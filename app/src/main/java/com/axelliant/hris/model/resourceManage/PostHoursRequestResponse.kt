package com.axelliant.hris.model.resourceManage

import com.axelliant.hris.model.base.Meta


data class PostHoursRequestResponse(
    val resource_hour_data: DocumentHours?=null,
    val status_message:String?=null,
    val meta: Meta
)