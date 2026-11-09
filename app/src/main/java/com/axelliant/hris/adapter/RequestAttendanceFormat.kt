package com.axelliant.hris.adapter

/** Extracts display-only date/clock parts without converting or changing the API value. */
internal object RequestAttendanceFormat {
    /** Returns the original date part, or -- when the value contains no date. */
    fun date(value: String?): String = split(value).first

    /** Displays the received clock to minute precision, retaining its AM/PM representation if present. */
    fun time(value: String?): String {
        val clock = split(value).second
        val minutes = CLOCK.find(clock)?.value ?: return "--"
        val suffix = MERIDIEM.find(clock)?.value
        return if (suffix == null) minutes else "$minutes $suffix"
    }

    /** Displays creation as yyyy-MM-dd HH:mm without timezone conversion or source-value mutation. */
    fun submittedOn(value: String?): String {
        val date = date(value)
        if (date == "--") return "--"
        val time = time(value)
        return if (time == "--") date else "$date $time"
    }

    private fun split(value: String?): Pair<String, String> {
        val text = value?.trim().orEmpty()
        val parts = text.split(Regex("[T\\s]+"), limit = 2)
        val date = parts.firstOrNull()?.takeIf { it.matches(DATE) }
        val time = if (date != null) parts.getOrNull(1) else text
        return (date ?: "--") to (time?.takeIf { CLOCK.containsMatchIn(it) } ?: "--")
    }

    private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val CLOCK = Regex("^\\d{1,2}:\\d{2}\\b")
    private val MERIDIEM = Regex("\\b(?:AM|PM)\\b", RegexOption.IGNORE_CASE)
}
