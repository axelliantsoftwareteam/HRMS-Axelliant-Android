package com.axelliant.hrms.model.leave

import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel

data class MyUpcomingLeaveDetailResponse(
    val upcoming_leaves: ArrayList<UpcomingLeaves>? = null,
    val meta: Meta
)