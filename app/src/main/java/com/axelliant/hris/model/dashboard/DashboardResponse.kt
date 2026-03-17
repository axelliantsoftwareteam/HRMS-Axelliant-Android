package com.axelliant.hris.model.dashboard
import com.axelliant.hris.model.attendance.ShiftData
import com.axelliant.hris.model.base.Meta

data class DashboardResponse(
        val birthday_data: ArrayList<Birthday>?=null,
        val employee_profile:EmployProfile?=null,
        val checkin_info:CheckInInfoResponse?=null,
        val branch_data:  ArrayList<BranchDataResponse>?=null,
        val shift_detail: ShiftData?=null,
        val meta: Meta
)