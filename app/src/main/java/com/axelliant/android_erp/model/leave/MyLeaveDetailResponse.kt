package com.axelliant.android_erp.model.leave

import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.dashboard.FilterModel

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val leave_status: ArrayList<FilterModel>? = null,
    val meta: Meta
)