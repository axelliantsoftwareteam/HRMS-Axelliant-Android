package com.axelliant.android_erp.model.leave

import com.axelliant.android_erp.model.base.Meta

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val meta: Meta
)