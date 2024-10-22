package com.axelliant.hris.model.leave.leaveCount

data class Annual(
    val expired_leaves: Int,
    val leaves_pending_approval: Double,
    val leaves_taken: Double,
    val remaining_leaves: Double,
    val total_leaves: Double
)