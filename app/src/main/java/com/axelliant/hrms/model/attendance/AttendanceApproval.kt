package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.EmployProfile

data class AttendanceApproval(
        val checkin: ArrayList<AttendanceApprovalObject>?=null,
        val meta: Meta
)