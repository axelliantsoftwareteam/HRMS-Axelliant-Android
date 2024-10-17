package com.axelliant.hris.model.leave.leaveCount

import com.axelliant.hris.model.base.Meta

data class GetLeaveCount(
    val leaves: Leaves?=null,
    val meta: Meta
)