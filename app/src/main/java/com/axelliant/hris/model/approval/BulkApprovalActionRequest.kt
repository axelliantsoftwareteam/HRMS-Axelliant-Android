package com.axelliant.hris.model.approval

data class BulkApprovalActionRequest(
    val actions: List<ApprovalActionItem> = emptyList(),
    val status: String,
    val comment: String? = null
)

data class ApprovalActionItem(
    val approval_type: String,
    val reference_name: String
)
