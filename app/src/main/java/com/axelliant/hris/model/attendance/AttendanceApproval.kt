package com.axelliant.hris.model.attendance
import com.axelliant.hris.model.base.Meta

data class AttendanceApproval(
        val checkin: ArrayList<AttendanceApprovalObject>?=null,
        val meta: Meta
)