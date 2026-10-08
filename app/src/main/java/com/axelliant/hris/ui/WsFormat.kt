package com.axelliant.hris.ui

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Display formatting shared by the redesigned screens (mirrors the iOS `WorkspaceFormat`).
 *
 * HRIS timestamps without an offset are in Asia/Karachi; they are shown in the device time zone.
 * Date-only values stay in the Karachi zone so the calendar day never shifts.
 * Uses `SimpleDateFormat` only (the app supports API 24, no java.time).
 */
object WsFormat {

    private val KARACHI: TimeZone = TimeZone.getTimeZone("Asia/Karachi")
    const val MEDIA_HOST = "https://hris.axelliant.com/"

    private val dateTimePatterns = listOf(
        "yyyy-MM-dd HH:mm:ss.SSSSSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm"
    )
    private val offsetPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    /** Parses an HRIS timestamp/date string; null when it cannot be read. */
    fun parse(value: String?): Date? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        for (p in offsetPatterns) {
            runCatching { return SimpleDateFormat(p, Locale.US).parse(text) }
        }
        for (p in dateTimePatterns) {
            runCatching {
                val f = SimpleDateFormat(p, Locale.US).apply { timeZone = KARACHI }
                return f.parse(text)
            }
        }
        runCatching {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = KARACHI }
            return f.parse(text)
        }
        return null
    }

    private fun hasTime(value: String): Boolean = value.trim().length > 10

    /** "30 Sep 2026" (empty if unreadable). */
    fun date(value: String?): String {
        val text = value?.trim().orEmpty()
        val d = parse(text) ?: return ""
        val f = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        f.timeZone = if (hasTime(text)) TimeZone.getDefault() else KARACHI
        return f.format(d)
    }

    /** "9:02 AM" or "09:02" depending on the phone's 12/24 h setting. Never shows seconds. */
    fun time(context: Context, value: String?): String {
        val text = value?.trim().orEmpty()
        val d = parse(text) ?: return clockText(context, text)
        return formatClock(context, d)
    }

    /** Re-formats clock-only text such as "09:02:15 PM"/"21:02:15"; unparseable text ("--:--", "Weekend") is returned as is. */
    fun clockText(context: Context, text: String?): String {
        val raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return raw
        val patterns = listOf("hh:mm:ss a", "h:mm:ss a", "HH:mm:ss", "HH:mm", "hh:mm a", "h:mm a")
        for (p in patterns) {
            runCatching {
                val f = SimpleDateFormat(p, Locale.US)
                f.isLenient = false
                val d = f.parse(raw) ?: return@runCatching
                return formatClock(context, d)
            }
        }
        return raw
    }

    private fun formatClock(context: Context, d: Date): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(d)
    }

    /** "30 Sep 2026, 9:02 AM". */
    fun dateTime(context: Context, value: String?): String {
        val d = date(value)
        val t = time(context, value)
        return when {
            d.isEmpty() -> t
            t.isEmpty() || !hasTime(value.orEmpty()) -> d
            else -> "$d, $t"
        }
    }

    /** "19 Feb 2025 at 7:23 PM". */
    fun dateAt(context: Context, value: String?): String {
        val d = date(value)
        val t = time(context, value)
        return if (t.isEmpty() || !hasTime(value.orEmpty())) d else "$d at $t"
    }

    /** "5 Sep 2026 – 7 Sep 2026", or a single date when both ends match. */
    fun dateRange(from: String?, to: String?): String {
        val a = date(from)
        val b = date(to)
        return when {
            a.isEmpty() -> b
            b.isEmpty() || a == b -> a
            else -> "$a – $b"
        }
    }

    /** "30-09-2026" (or "30-09-2026 18:10" when the value has a time). */
    fun reversedDay(value: String?): String {
        val text = value?.trim().orEmpty()
        val d = parse(text) ?: return ""
        val withTime = hasTime(text)
        val f = SimpleDateFormat(if (withTime) "dd-MM-yyyy HH:mm" else "dd-MM-yyyy", Locale.getDefault())
        f.timeZone = if (withTime) TimeZone.getDefault() else KARACHI
        return f.format(d)
    }

    /** "8.5 hrs" – one decimal, empty when 0. */
    fun hours(value: Double?): String =
        if (value == null || value == 0.0) "" else String.format(Locale.US, "%.1f hrs", value)

    /** "1 day", "2.5 days", "3 days" – empty when 0. */
    fun days(value: Double?): String {
        if (value == null || value == 0.0) return ""
        val n = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
        return if (value == 1.0) "$n day" else "$n days"
    }

    /** Whole numbers print as "7", others as "1.5". */
    fun number(value: Double?): String {
        val v = value ?: 0.0
        return if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
    }

    /** "yyyy-MM-dd" for API requests. */
    fun apiDay(date: Date = Date()): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun apiDay(calendar: Calendar): String = apiDay(calendar.time)

    /** Server date for a display date in "dd-MM-yyyy". */
    fun displayToApi(display: String?): String {
        val d = runCatching { SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(display.orEmpty()) }.getOrNull()
        return d?.let { apiDay(it) }.orEmpty()
    }

    /** Photo paths without a scheme are relative to the HRIS host. */
    fun mediaUrl(path: String?): String {
        val p = path?.trim().orEmpty()
        return when {
            p.isEmpty() -> ""
            p.startsWith("http", ignoreCase = true) -> p
            else -> MEDIA_HOST + p.trimStart('/')
        }
    }

    /** First letters of the first two words, upper-cased ("Umer Azhar" → "UA"). */
    fun initials(name: String?): String =
        name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}
