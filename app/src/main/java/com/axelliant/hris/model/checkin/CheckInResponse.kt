package com.axelliant.hris.model.checkin
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel

data class CheckInListResponse(
    val checkin: ArrayList<CheckInDetail>?=null,
    val checkin_count:Int=0,
    val checkin_status: ArrayList<FilterModel>?=null,
    val meta: Meta
)