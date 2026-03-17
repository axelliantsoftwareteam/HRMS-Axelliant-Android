package com.axelliant.hris.model.leave

import com.axelliant.hris.model.base.Meta

data class UpcomingLeaves(
    val from_date: String?=null,
    val leave_type: String?=null,
    val status: String?=null,
    val to_date: String?=null,
    val total_leave_days: Double?=null,
    val unix_from_date: Double?=null,
    val unix_to_date: Double?=null,
    val reason: String?=null,
    val meta: Meta
)
