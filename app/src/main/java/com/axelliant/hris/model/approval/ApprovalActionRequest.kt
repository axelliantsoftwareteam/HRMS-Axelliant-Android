package com.axelliant.hris.model.approval

data class ApprovalActionRequest(
    var approval_type: String = "",
    var reference_name: String = "",
    var status: String = "",
    var comment: String? = null
)
