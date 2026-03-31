package com.axelliant.hris.model.resourceManage

data class AddResourceType(
    var working_hours: Double?=0.0,
    var project_id: String="",
    var project: String="",
    var name: String="",
    var date: String?=null,
//    var expenseTypeList: ArrayList<ProjectType> = arrayListOf()

)

