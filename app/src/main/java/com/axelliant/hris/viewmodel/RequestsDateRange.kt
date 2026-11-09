package com.axelliant.hris.viewmodel

import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.RequestsOverviewViewModel.Period
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Converts date-only picker values without shifting days into the device's local timezone. */
internal object RequestsDateRange {
    /** Returns API date bounds for [period] relative to [today], leaving the supplied clock unchanged. */
    fun preset(period: Period, today: Calendar): Pair<String, String> {
        val start = today.clone() as Calendar
        val end = today.clone() as Calendar
        when (period) {
            Period.LAST_7_DAYS -> start.add(Calendar.DAY_OF_YEAR, -6)
            Period.THIS_MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            Period.CUSTOM -> error("Custom requires selected dates")
        }
        return WsFormat.apiDay(start) to WsFormat.apiDay(end)
    }

    /** Formats UTC picker bounds; reversed selections throw [IllegalArgumentException]. */
    fun selected(start: Long, end: Long): Pair<String, String> {
        require(start <= end) { "Start date must not follow end date" }
        val format = formatter()
        return format.format(java.util.Date(start)) to format.format(java.util.Date(end))
    }

    /** Returns UTC midnight for an API [day], or null for an invalid/incomplete date. */
    fun utcMillis(day: String): Long? {
        val position = ParsePosition(0)
        return formatter().parse(day, position)?.takeIf { position.index == day.length }?.time
    }

    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
}
