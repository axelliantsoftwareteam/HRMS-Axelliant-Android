package com.axelliant.hris.model.approvals

/** A page of `get_approvals`. */
data class ApprovalsPage(
    val items: List<ApprovalItem>? = null,
    val total: Any? = null
)

/** One request waiting for the manager. */
data class ApprovalItem(
    val approval_type: String? = null,
    val reference_name: String? = null,
    val employee_name: String? = null,
    val employee_code: String? = null,
    val custom_employee_code: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val location: String? = null,
    val effective_on: String? = null,
    val effective_on_iso: String? = null,
    val status: String? = null,
    val image: String? = null
) {
    /** Only "Pending" requests (the default when no status is sent) are listed. */
    val isPending: Boolean get() = (status ?: "Pending").equals("Pending", ignoreCase = true)

    val displayName: String
        get() = employee_name?.takeIf { it.isNotBlank() } ?: title?.takeIf { it.isNotBlank() }
            ?: reference_name?.takeIf { it.isNotBlank() } ?: "Employee"

    /** "Leave Application", "Checkin", "Expense Claim" ... */
    val displayType: String
        get() = approval_type?.takeIf { it.isNotBlank() }
            ?.replace('_', ' ')
            ?.split(' ')
            ?.filter { it.isNotEmpty() }
            ?.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
            ?: "Request"

    val displayStatus: String get() = status?.takeIf { it.isNotBlank() } ?: "Pending"

    val key: String
        get() = reference_name ?: "${employee_name.orEmpty()}|${effective_on.orEmpty()}|${title.orEmpty()}"
}

/** `bulk_take_approval_action` result: counts and each request's own outcome. */
data class BulkApprovalResult(
    val status_message: String? = null,
    val succeeded_count: Any? = null,
    val failed_count: Any? = null,
    val items: List<BulkApprovalItemResult>? = null,
    val meta: ApprovalMeta? = null
) {
    val succeeded: Int get() = (succeeded_count as? Number)?.toInt() ?: succeeded_count?.toString()?.toDoubleOrNull()?.toInt() ?: 0
    val failed: Int get() = (failed_count as? Number)?.toInt() ?: failed_count?.toString()?.toDoubleOrNull()?.toInt() ?: 0

    /** The first failed request's reason, if any. */
    val firstFailure: String?
        get() = items.orEmpty().firstOrNull { it.meta?.status == false }?.meta?.message
}

data class BulkApprovalItemResult(
    val reference_name: String? = null,
    val meta: ApprovalMeta? = null
)

data class ApprovalMeta(
    val message: String? = null,
    val status: Boolean? = null
)

/** Result of `take_approval_action` (only the optional message matters). */
data class ApprovalActionResult(
    val status_message: String? = null
)
