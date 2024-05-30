package com.axelliant.hrms.model.leave

import com.axelliant.hrms.model.base.Meta
import java.lang.reflect.Member

data class TeamLeaveDetailResponse(
    val leaves: ArrayList<TeamLeaveDetail>? = null,
    val totalMember: Int=0,
    val meta: Meta
)