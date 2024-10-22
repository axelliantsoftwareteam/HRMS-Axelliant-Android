package com.axelliant.hris.model.attendance
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.dashboard.EmployProfile

data class AttendanceResponse(
    val attendance_data: ArrayList<AttendanceDetail>?=null,
    val employee_list_: ArrayList<EmployProfile>?=null,
    val attendance_status: ArrayList<FilterModel>?=null,
    val meta: Meta
)