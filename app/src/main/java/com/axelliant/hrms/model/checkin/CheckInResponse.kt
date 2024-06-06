package com.axelliant.hrms.model.checkin
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.model.dashboard.EmployProfile

data class CheckInListResponse(
    val checkin: ArrayList<CheckInDetail>?=null,
    val checkin_count:Int=0,
    val checkin_status: ArrayList<FilterModel>?=null,
    val meta: Meta
)