package com.axelliant.hrms.model.leave

import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel
import java.lang.reflect.Member

data class TeamLeaveDetailResponse(
    val leaves: ArrayList<TeamLeaveDetail>? = null,
    val leave_status: ArrayList<FilterModel>? = null,
    val team_count: Int=0,
    val meta: Meta
)