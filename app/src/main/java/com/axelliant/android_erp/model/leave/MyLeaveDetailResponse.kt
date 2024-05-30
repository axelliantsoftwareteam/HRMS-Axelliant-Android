package com.axelliant.android_erp.model.leave

import com.axelliant.android_erp.model.base.Meta
import com.axelliant.android_erp.model.dashboard.AttendanceStatus

data class MyLeaveDetailResponse(
    val leaves: ArrayList<LeaveDetail>? = null,
    val leave_status: ArrayList<AttendanceStatus>? = null,
    val meta: Meta
)