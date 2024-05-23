package com.axelliant.android_erp.model.leave
import com.axelliant.android_erp.model.attendance.SelfAttendanceStats
import com.axelliant.android_erp.model.attendance.TeamAttendanceStats
import com.axelliant.android_erp.model.base.Meta

data class LeaveResponse(
        val self_attendance_counts: SelfAttendanceStats?=null,
        val team_attendance_counts: TeamAttendanceStats?=null,
        val meta: Meta
)