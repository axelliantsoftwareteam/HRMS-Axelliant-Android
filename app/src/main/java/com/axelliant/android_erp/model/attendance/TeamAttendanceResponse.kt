package com.axelliant.android_erp.model.attendance
import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.dashboard.EmployProfile

data class TeamAttendanceResponse(
        val attendance_data: ArrayList<AttendanceData>?=null,
        val meta: Meta
)