package com.axelliant.hris.model.leave

data class LeaveDetail(
    val description: String?=null,
    val employee_name: String,
    val from_date: String,
    val is_paid: Boolean,
    val leave_type: String,
    val name: String,
    val status: String,
    val to_date: String,
    val total_leave_days: Double,
    val leave_reason: String?=null,
    var isDetailVisible:Boolean=false
)