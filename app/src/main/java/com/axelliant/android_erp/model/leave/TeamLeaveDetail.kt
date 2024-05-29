package com.axelliant.android_erp.model.leave

data class TeamLeaveDetail(
    val employee_name: String,
    val name: String,
    val designation: String,
    val post_date: String,
    val from_date: String,
    val to_date: String,
    val leave_type: String,
    val leave_reason: String?=null,
    val leave_approver: String?=null,
    val status: String,
)