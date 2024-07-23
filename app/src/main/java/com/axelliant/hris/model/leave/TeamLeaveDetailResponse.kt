package com.axelliant.hris.model.leave

import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel

data class TeamLeaveDetailResponse(
    val leaves: ArrayList<TeamLeaveDetail>? = null,
    val leave_status: ArrayList<FilterModel>? = null,
    val team_count: Int=0,
    val meta: Meta
)