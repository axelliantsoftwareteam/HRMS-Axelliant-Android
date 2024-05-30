package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.model.dashboard.EmployProfile

data class AttendanceResponse(
    val attendance_data: ArrayList<AttendanceDetail>?=null,
    val employee_list_: ArrayList<EmployProfile>?=null,
    val attendance_status: ArrayList<FilterModel>?=null,
    val meta: Meta
)