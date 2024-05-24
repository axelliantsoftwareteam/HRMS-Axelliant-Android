package com.axelliant.android_erp.model.leave
import com.axelliant.android_erp.model.base.Meta

data class LeaveResponse(
        val self_count: SelfLeaveStats?=null,
        val team_count: TeamLeaveStats?=null,
        val remaining_balance: ArrayList<LeaveType>?=null,
        val meta: Meta
)