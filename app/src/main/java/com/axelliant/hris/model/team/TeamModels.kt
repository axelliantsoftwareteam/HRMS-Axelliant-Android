package com.axelliant.hris.model.team

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** `get_team_attendance` payload: each member's counts and hours for the date range. */
data class TeamAttendanceData(
    val attendance_data: List<TeamMember>? = null,
    val team_count: Any? = null
)

/** One member's working-hours summary plus HRIS's named metrics (Present, Absent, Total Hrs, ...). */
data class TeamMember(
    val id: String? = null,
    val name: String? = null,
    val designation: String? = null,
    val image: String? = null,
    val values: List<TeamMetric>? = null
) {
    /** Stable across reloads: the employee id, else the name. */
    val identity: String get() = id ?: name.orEmpty()

    /** The count HRIS sent for a named metric, or "0". Counts arrive as numbers or text. */
    fun metric(title: String): String {
        val raw = values?.firstOrNull { it.name == title }?.count ?: return "0"
        val text = when (raw) {
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            else -> raw.toString()
        }
        return text.ifBlank { "0" }
    }
}

data class TeamMetric(
    val name: String? = null,
    val count: Any? = null
)

/** `get_team_checkin_detail_list` payload. */
data class TeamCheckinData(
    val employee_list: List<TeamArrival>? = null
)

/** One team member's check-in. */
data class TeamArrival(
    val employee: String? = null,
    val employee_name: String? = null,
    val image: String? = null,
    val date: String? = null,
    val time_iso: String? = null,
    val location: String? = null,
    val status: String? = null
)

/** `get_leaves_detail_team` payload. */
data class TeamQuotaData(
    val employees: List<TeamQuotaEmployee>? = null
)

data class TeamQuotaEmployee(
    val employee: String? = null,
    val employee_name: String? = null,
    val designation: String? = null,
    val image: String? = null,
    val employee_code: String? = null,
    val is_self: Any? = null,
    val leaves: TeamQuotaLeaves? = null
) {
    val isSelf: Boolean get() = TeamNum.bool(is_self)

    /** Total days available across all leave types. */
    val totalAvailable: Double
        get() = leaves?.leave_allocation.orEmpty().sumOf { it.availableDays }

    /** A key for the member's card: employee id, else code, else the position in the list. */
    fun quotaId(index: Int): String =
        employee?.takeIf { it.isNotEmpty() } ?: employee_code?.takeIf { it.isNotEmpty() } ?: "team-member-$index"
}

data class TeamQuotaLeaves(
    val leave_allocation: List<TeamQuotaAllocation>? = null
)

data class TeamQuotaAllocation(
    val name: String? = null,
    val total_leaves: Any? = null,
    val expired_leaves: Any? = null,
    val leaves_taken: Any? = null,
    val leaves_pending_approval: Any? = null,
    val remaining_leaves: Any? = null,
    val available: Any? = null,
    val used: Any? = null
) {
    val totalDays: Double get() = TeamNum.num(total_leaves) ?: 0.0
    val usedDays: Double get() = TeamNum.num(used) ?: TeamNum.num(leaves_taken) ?: 0.0
    val pendingDays: Double get() = TeamNum.num(leaves_pending_approval) ?: 0.0
    val availableDays: Double get() = TeamNum.num(available) ?: TeamNum.num(remaining_leaves) ?: 0.0
}

/** Lenient number/boolean reading for fields HRIS sends as numbers, numeric text, 0/1 or booleans. */
object TeamNum {
    fun num(value: Any?): Double? = when (value) {
        null -> null
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }

    fun int(value: Any?): Int? = num(value)?.toInt()

    fun bool(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", true) || value == "1"
        else -> false
    }
}

/** `yyyy-MM-dd` helpers shared by the Team Activity and Team Calendar filters. */
object TeamDates {
    private fun fmt(utc: Boolean) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }

    fun today(): String = fmt(false).format(Calendar.getInstance().time)

    fun firstOfMonth(cal: Calendar = Calendar.getInstance()): String {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        return fmt(false).format(c.time)
    }

    fun lastOfMonth(cal: Calendar = Calendar.getInstance()): String {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        return fmt(false).format(c.time)
    }

    /** Local calendar for a `yyyy-MM-dd` string, or now when unreadable. */
    fun calendar(day: String?): Calendar {
        val c = Calendar.getInstance()
        runCatching { fmt(false).parse(day.orEmpty()) }.getOrNull()?.let { c.time = it }
        return c
    }

    /** Millis for MaterialDatePicker (UTC midnight of the given day). */
    fun toPickerMillis(day: String?): Long =
        runCatching { fmt(true).parse(day.orEmpty())?.time }.getOrNull() ?: System.currentTimeMillis()

    /** Day string for a MaterialDatePicker selection (UTC midnight). */
    fun fromPickerMillis(millis: Long): String = fmt(true).format(java.util.Date(millis))
}
