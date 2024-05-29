package com.axelliant.android_erp.model.leave

import com.axelliant.android_erp.model.base.Meta
import java.lang.reflect.Member

data class TeamLeaveDetailResponse(
    val leaves: ArrayList<TeamLeaveDetail>? = null,
    val totalMember: Int=0,
    val meta: Meta
)