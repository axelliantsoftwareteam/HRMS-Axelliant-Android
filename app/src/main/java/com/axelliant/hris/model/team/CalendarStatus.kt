package com.axelliant.hris.model.team

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.axelliant.hris.R

/**
 * A day's attendance category in the Team Calendar, with its colour and short mark. Declared in the
 * legend order: Office (Present), Work From Home, Leave, Absent, Weekend, Holiday, No Record.
 */
enum class CalendarStatus(@StringRes val labelRes: Int, @ColorRes val colorRes: Int, val mark: String) {
    OFFICE(R.string.cal_status_office, R.color.ws_green, "P"),
    WORK_FROM_HOME(R.string.cal_status_wfh, R.color.ws_bittersweet, "WFH"),
    LEAVE(R.string.cal_status_leave, R.color.ws_amber, "L"),
    ABSENT(R.string.cal_status_absent, R.color.ws_red, "A"),
    WEEKEND(R.string.cal_status_weekend, R.color.ws_weekend, "W"),
    HOLIDAY(R.string.cal_status_holiday, R.color.ws_secondary, "H"),
    NO_RECORD(R.string.cal_status_no_record, R.color.ws_grey, "–");

    companion object {
        /** Classifies a day by its label (or status when it has no label). */
        fun of(day: TeamCalendarDay): CalendarStatus {
            val text = (day.label?.takeIf { it.isNotEmpty() } ?: day.status.orEmpty()).lowercase()
            return when {
                text.isEmpty() -> NO_RECORD
                text.contains("work from home") || text == "wfh" -> WORK_FROM_HOME
                text.contains("absent") -> ABSENT
                text.contains("holi") -> HOLIDAY
                text.contains("weekend") || text.contains("wknd") -> WEEKEND
                text.contains("leave") -> LEAVE
                text.contains("office") || text.contains("present") -> OFFICE
                else -> NO_RECORD
            }
        }
    }
}
