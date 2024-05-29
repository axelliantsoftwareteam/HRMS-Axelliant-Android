package com.axelliant.hrms.model.leave

import com.axelliant.hrms.model.base.Meta

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val meta: Meta
)