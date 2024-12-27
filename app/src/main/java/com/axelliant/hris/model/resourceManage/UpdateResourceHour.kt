package com.axelliant.hris.model.resourceManage

data class UpdateResourceHour(
    var resource_hour_id: String?=null,
    var resource_hour_detail: ArrayList<AddResourceType>?=null,
)