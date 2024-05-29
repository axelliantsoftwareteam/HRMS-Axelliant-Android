package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta

data class TeamAttendanceResponse(
        val attendance_data: ArrayList<AttendanceData>?=null,
        val meta: Meta
)