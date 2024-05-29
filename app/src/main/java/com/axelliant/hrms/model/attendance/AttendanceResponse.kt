package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.EmployProfile

data class AttendanceResponse(
        val attendance_data: ArrayList<AttendanceDetail>?=null,
        val employee_list_: ArrayList<EmployProfile>?=null,
        val meta: Meta
)