package com.axelliant.hris.model.leave.leaveCount
data class LeaveAllocation(
    var name: String?=null,
    val team_count: Double?=null,
    val expired_leaves: Double? = null,
    val leaves_taken: Double? = null,
    val leaves_pending_approval: Double? = null,
    var remaining_leaves: Double? = null
)