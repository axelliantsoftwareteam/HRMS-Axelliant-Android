package com.axelliant.android_erp.model.attendance
import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.dashboard.FilterModel
import com.axelliant.android_erp.model.dashboard.EmployProfile

data class AttendanceResponse(
    val attendance_data: ArrayList<AttendanceDetail>?=null,
    val employee_list_: ArrayList<EmployProfile>?=null,
    val attendance_status: ArrayList<FilterModel>?=null,
    val meta: Meta
)