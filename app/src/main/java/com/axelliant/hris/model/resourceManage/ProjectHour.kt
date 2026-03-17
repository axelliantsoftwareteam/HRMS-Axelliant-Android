package com.axelliant.hris.model.resourceManage

import com.axelliant.hris.model.leave.SpinnerType

class ProjectHour {
    var date: String?=null
    var name: String?=null
    var project: String?=null
    var project_name: String?=null
    var working_hours: Double?=null
    var expenseTypeList: ArrayList<ProjectType> = arrayListOf()

}