package com.axelliant.hris.model.attendance
import com.axelliant.hris.model.base.Meta

data class TeamAttendanceResponse(
        val attendance_data: ArrayList<AttendanceData>?=null,
        val team_count:Int =0,
        val meta: Meta
)