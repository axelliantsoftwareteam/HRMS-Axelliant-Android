package com.axelliant.hrms.model.leave

import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val leave_status: ArrayList<FilterModel>? = null,
    val meta: Meta
)