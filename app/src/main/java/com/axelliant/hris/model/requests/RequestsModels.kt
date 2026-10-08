package com.axelliant.hris.model.requests

/**
 * HRIS sends numbers as JSON numbers or as quoted strings (sometimes empty). Fields that can arrive
 * either way are declared `Any?` (Gson gives a Double or a String) and read through this.
 */
fun Any?.toAmount(): Double = when (this) {
    is Number -> toDouble()
    is String -> trim().toDoubleOrNull() ?: 0.0
    else -> 0.0
}

// ---- get_leave_application_requests -> data.leaves ----

data class LeaveRequestItem(
    val name: String? = null,
    val leave_type: String? = null,
    val leave_reason: String? = null,
    val from_date: String? = null,
    val to_date: String? = null,
    val total_leave_days: Any? = null,
    val status: String? = null,
    val post_date: String? = null
)

data class LeaveRequestsPayload(val leaves: List<LeaveRequestItem>? = null)

// ---- get_employee_checkins -> data.checkin ----

data class CheckinRequestItem(
    val name: String? = null,
    val requeststatus: String? = null,
    val log_type: String? = null,
    val time: String? = null,
    val location: String? = null,
    val reason: String? = null,
    val working_hours: Any? = null,
    val creation: String? = null
)

data class CheckinRequestsPayload(val checkin: List<CheckinRequestItem>? = null)

/** One row of the Requests overview (a check-in, a leave application or an expense claim). */
data class RequestRow(
    val kind: Kind,
    val reference: String,
    /** "CHECK IN" / "CHECK OUT" for check-ins, the leave type for leave, empty for expenses. */
    val rawTitle: String,
    val reason: String,
    val status: String,
    val location: String,
    /** The moment the request is about: a check-in's time, a leave's first day or a claim's posting date. */
    val effectiveOn: String,
    val fromDate: String,
    val toDate: String,
    val workingHours: Double,
    val totalDays: Double,
    val totalAmount: Double,
    val submittedOn: String
) {
    enum class Kind { CHECKIN, LEAVE, EXPENSE }

    /** Stable identity for list diffing. */
    val key: String get() = "$kind-$reference-$effectiveOn"
}
