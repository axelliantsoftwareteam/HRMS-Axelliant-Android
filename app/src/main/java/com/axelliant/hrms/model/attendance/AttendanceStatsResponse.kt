package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta

data class AttendanceStatsResponse(
        val self_attendance_counts: SelfAttendanceStats?=null,
        val team_attendance_counts:TeamAttendanceStats?=null,
        val meta: Meta
)