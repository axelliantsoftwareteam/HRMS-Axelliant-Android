package com.axelliant.hris.model.leaveatt

import com.axelliant.hris.model.dashboard.FilterModel

/*
 * Models for the Leave, Request (leave / attendance forms) and Attendance screens.
 * Everything is nullable with defaults, and numbers arrive as String? because HRIS sometimes
 * sends "" or a quoted number (Gson cannot read "" as Double). Use the helpers to read them.
 */

/** Parses a loosely typed number ("1.5", "", null) to a Double, 0.0 when it is unreadable. */
fun String?.toNumber(): Double = this?.trim()?.toDoubleOrNull() ?: 0.0

// ---- Leave quotas (get_leaves_detail) ----------------------------------------------------------

data class LeaveQuota(
    val name: String? = null,
    val total_leaves: String? = null,
    val leaves_taken: String? = null,
    val leaves_pending_approval: String? = null,
    val remaining_leaves: String? = null
) {
    val total: Double get() = total_leaves.toNumber()
    val remaining: Double get() = remaining_leaves.toNumber()
}

data class LeaveQuotaBlock(val leave_allocation: List<LeaveQuota>? = null)

data class LeaveQuotaPayload(val leaves: LeaveQuotaBlock? = null) {
    val allocations: List<LeaveQuota> get() = leaves?.leave_allocation.orEmpty()
}

// ---- Leave history (get_leave_application_requests) --------------------------------------------

/**
 * One leave request. Field names match the HRIS JSON (and the old `LeaveDetail` model), so the same
 * JSON travels to the edit form in the navigation bundle.
 */
data class LeaveRecord(
    val name: String? = null,
    val employee_name: String? = null,
    val leave_type: String? = null,
    val from_date: String? = null,
    val to_date: String? = null,
    val status: String? = null,
    val leave_reason: String? = null,
    val description: String? = null,
    val leave_approver: String? = null,
    val post_date: String? = null,
    val total_leave_days: String? = null,
    val is_paid: Any? = null
) {
    /** HRIS's status as sent, "Open" when it sends none. */
    val rawStatus: String get() = status?.takeIf { it.isNotBlank() } ?: "Open"

    val days: Double get() = total_leave_days.toNumber()

    /** null when HRIS did not say. */
    val paid: Boolean?
        get() = when (val v = is_paid) {
            null -> null
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> v.equals("true", true) || v == "1"
            else -> null
        }

    /** Reason as typed by the employee ("leave_reason", or the older "description"). */
    val reason: String get() = leave_reason?.takeIf { it.isNotBlank() } ?: description.orEmpty()
}

data class LeaveHistoryPayload(val leaves: List<LeaveRecord>? = null)

data class LeaveDaysPayload(val days: String? = null)

// ---- Request forms ----------------------------------------------------------------------------

data class CheckinOption(val type: String? = null)

data class CheckinOptionsPayload(
    val checkin: List<CheckinOption>? = null,
    val location: List<CheckinOption>? = null
)

/** Response of create / update / delete calls: only the message matters. */
data class ActionPayload(val status_message: String? = null)

/** The attendance request handed to the edit form (same field names as the old `CheckInDetail`). */
data class AttendanceRequestRecord(
    val name: String? = null,
    val time: String? = null,
    val log_type: String? = null,
    val location: String? = null,
    val reason: String? = null,
    val requeststatus: String? = null
)

// ---- Attendance (get_filtered_attendance, get_week_attendance_stats) ---------------------------

data class AttendanceDay(
    val status: String? = null,
    val display_status: String? = null,
    val in_time: String? = null,
    val out_time: String? = null,
    val in_time_iso: String? = null,
    val out_time_iso: String? = null,
    val date: String? = null,
    val working_hours: String? = null,
    val expected_in: String? = null,
    val expected_out: String? = null,
    val expected_in_iso: String? = null,
    val expected_out_iso: String? = null,
    val shift: String? = null,
    val shift_timings: String? = null,
    val custom_attendance_status: String? = null
) {
    /** Status as people read it: the display status when present. */
    val shownStatus: String get() = display_status?.takeIf { it.isNotBlank() } ?: status.orEmpty()
    val hours: Double get() = working_hours.toNumber()
}

data class AttendancePayload(
    val attendance_data: List<AttendanceDay>? = null,
    val attendance_status: List<FilterModel>? = null
)

data class ShiftDetail(
    val name: String? = null,
    val location: String? = null,
    val actual_start: String? = null,
    val actual_end: String? = null
)

data class WeekStatsPayload(val shift_detail: ShiftDetail? = null)
