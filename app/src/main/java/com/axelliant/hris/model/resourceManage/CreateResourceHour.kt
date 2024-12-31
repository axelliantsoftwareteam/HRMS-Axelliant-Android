package com.axelliant.hris.model.resourceManage

data class CreateResourceHour(
    var name:String?=null,
    var project_hours: ArrayList<ProjectHour>?=null,
)