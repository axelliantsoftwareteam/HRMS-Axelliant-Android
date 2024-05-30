package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.EmployProfile

data class TeamAttendanceResponse(
        val attendance_data: ArrayList<AttendanceData>?=null,
        val meta: Meta
)