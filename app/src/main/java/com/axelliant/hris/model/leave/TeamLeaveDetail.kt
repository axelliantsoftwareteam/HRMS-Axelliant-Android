package com.axelliant.hris.model.leave

data class TeamLeaveDetail(
    val name: String,
    val employee_name: String,
    val from_date: String,
    val to_date: String,
    val leave_type: String,
    val leave_reason: String? = null,
    val leave_approver: String? = null,
    val designation: String,
    val image: String,
    val post_date: String,
    val status: String,
    val total_leave_days: Double = 0.0,
    val is_paid: Boolean = false

)