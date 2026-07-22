package com.axelliant.hris.model.leave

import com.axelliant.hris.model.base.Meta

data class TeamLeaveQuotaResponse(
    val employees: ArrayList<QuotaEmployee>? = null,
    val team_count: Int = 0,
    val as_of_date: String? = null,
    val meta: Meta
)

data class QuotaEmployee(
    val employee: String? = null,
    val employee_name: String? = null,
    val designation: String? = null,
    val image: String? = null,
    val employee_code: String? = null,
    val is_self: Boolean = false,
    val leaves: QuotaLeaves? = null
) {
    /** Total leaves available across all allocations (used for the "X available" badge). */
    val totalAvailable: Double
        get() = leaves?.leave_allocation?.sumOf { it.available } ?: 0.0
}

data class QuotaLeaves(
    val leave_allocation: ArrayList<LeaveAllocation>? = null
)

data class LeaveAllocation(
    val name: String? = null,
    val total_leaves: Double = 0.0,
    val expired_leaves: Double = 0.0,
    val leaves_taken: Double = 0.0,
    val leaves_pending_approval: Double = 0.0,
    val remaining_leaves: Double = 0.0,
    val available: Double = 0.0,
    val used: Double = 0.0
)
