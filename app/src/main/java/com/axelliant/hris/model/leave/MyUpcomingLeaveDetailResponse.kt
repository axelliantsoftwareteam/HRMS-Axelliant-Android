package com.axelliant.hris.model.leave

import com.axelliant.hris.model.base.Meta

data class MyUpcomingLeaveDetailResponse(
    val upcoming_leaves: ArrayList<UpcomingLeaves>? = null,
    val meta: Meta
)