package com.axelliant.hris.adapter

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.axelliant.hris.R
import com.axelliant.hris.model.leaveatt.AttendanceDay
import com.axelliant.hris.ui.WsFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** How an attendance day reads on its card and in its details sheet (mirrors the iOS attendance screen). */
object AttFormat {

    private val KARACHI: TimeZone = TimeZone.getTimeZone("Asia/Karachi")

    /** Raw texts HRIS sends in place of a clock time. */
    private val NO_TIME = setOf("weekend", "holiday", "check in missing", "check out missing")

    /** Absent red, Weekend purple, Holiday blue, everything else green. */
    @ColorInt
    fun statusColor(context: Context, status: String?): Int {
        val s = status.orEmpty()
        val id = when {
            s.equals("Absent", true) -> R.color.ws_red
            s.contains("Weekend", true) -> R.color.ws_weekend
            s.contains("Holiday", true) -> R.color.ws_secondary
            else -> R.color.ws_green
        }
        return ContextCompat.getColor(context, id)
    }

    /**
     * A clock time for display, or null when there is none. The ISO value (converted to the device
     * zone) wins; otherwise the raw text is re-formatted unless it is one of HRIS's placeholders.
     */
    fun timeText(context: Context, iso: String?, raw: String?): String? {
        if (!iso.isNullOrBlank() && WsFormat.parse(iso) != null) {
            return WsFormat.time(context, iso).takeIf { it.isNotBlank() }
        }
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.startsWith("--") || text.lowercase() in NO_TIME) return null
        return WsFormat.clockText(context, text)
    }

    /** "12:25 PM – 9:13 PM", "12:25 PM –", "– 9:13 PM" or "--". */
    fun timeWindow(context: Context, day: AttendanceDay): String {
        val actualIn = timeText(context, day.in_time_iso, day.in_time)
        val actualOut = timeText(context, day.out_time_iso, day.out_time)
        return when {
            actualIn != null && actualOut != null -> context.getString(R.string.att_time_range, actualIn, actualOut)
            actualIn != null -> context.getString(R.string.att_time_in_only, actualIn)
            actualOut != null -> context.getString(R.string.att_time_out_only, actualOut)
            else -> context.getString(R.string.att_none)
        }
    }

    /** "7.93 h", or "--" when nothing was worked. */
    fun hours(context: Context, day: AttendanceDay): String =
        day.hours.takeIf { it > 0 }?.let { context.getString(R.string.att_hours_value, it) } ?: context.getString(R.string.att_none)

    /** "Wed, 7 Oct" (the calendar day never shifts: the date is read in the HRIS zone). */
    fun dayHeading(date: String?): String = formatDay(date, "EEE, d MMM")

    /** "Oct 7, 2026". */
    fun sheetTitle(date: String?): String = formatDay(date, "MMM d, yyyy")

    private fun formatDay(date: String?, pattern: String): String {
        val parsed = WsFormat.parse(date) ?: return date.orEmpty()
        return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = KARACHI }.format(parsed)
    }
}
