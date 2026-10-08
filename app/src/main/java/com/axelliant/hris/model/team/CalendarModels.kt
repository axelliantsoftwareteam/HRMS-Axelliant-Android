package com.axelliant.hris.model.team

/** `get_team_calendar` payload. */
data class TeamCalendarData(
    val dates: List<String>? = null,
    val employees: List<TeamCalendarMember>? = null,
    val time_zone: String? = null
)

/** One team member and their days. */
data class TeamCalendarMember(
    val employee_id: String? = null,
    val employee_code: String? = null,
    val employee_name: String? = null,
    val designation: String? = null,
    val department: String? = null,
    val image: String? = null,
    val days: List<TeamCalendarDay>? = null
)

/** One member's attendance on one day. */
data class TeamCalendarDay(
    val date: String? = null,
    val label: String? = null,
    val status: String? = null,
    val in_time: String? = null,
    val in_time_iso: String? = null,
    val out_time: String? = null,
    val out_time_iso: String? = null,
    val working_hours: Any? = null,
    val location: String? = null,
    val shift: String? = null,
    val shift_timings: String? = null
)
