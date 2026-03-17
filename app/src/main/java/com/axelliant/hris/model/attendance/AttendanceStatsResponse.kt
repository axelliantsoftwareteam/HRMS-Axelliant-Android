package com.axelliant.hris.model.attendance
import com.axelliant.hris.model.base.Meta

data class AttendanceStatsResponse(
        val self_attendance_counts: SelfAttendanceStats?=null,
        val team_attendance_counts:TeamAttendanceStats?=null,
        val shift_detail:ShiftData?=null,
        val meta: Meta
)