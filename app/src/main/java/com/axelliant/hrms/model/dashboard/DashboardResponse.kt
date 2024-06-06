package com.axelliant.hrms.model.dashboard
import com.axelliant.hrms.model.base.Meta

data class DashboardResponse(
        val birthday_data: ArrayList<Birthday>?=null,
        val employee_profile:EmployProfile?=null,
        val checkin_info:CheckInInfoResponse?=null,
        val branch_data:  ArrayList<BranchDataResponse>?=null,
        val meta: Meta
)