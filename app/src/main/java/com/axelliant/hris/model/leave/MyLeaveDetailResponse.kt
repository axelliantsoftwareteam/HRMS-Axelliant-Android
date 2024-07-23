package com.axelliant.hris.model.leave

import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val leave_status: ArrayList<FilterModel>? = null,
    val meta: Meta
)