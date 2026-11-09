package com.axelliant.hris.adapter

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies request labels display the API date/clock components without reformatting them. */
class RequestAttendanceFormatTest {
    /** The exact reported API format is split at its space without adding seconds. */
    @Test
    fun minuteTimestampIsSplitForDisplayOnly() {
        val value = "2026-11-05 12:32"
        assertEquals("2026-11-05", RequestAttendanceFormat.date(value))
        assertEquals("12:32", RequestAttendanceFormat.time(value))
    }

    /** Display precision drops seconds/fractions while keeping the received date and clock minute. */
    @Test
    fun timestampDisplaysMinutePrecision() {
        val value = "2026-11-05 09:30:00.123456"
        assertEquals("2026-11-05", RequestAttendanceFormat.date(value))
        assertEquals("09:30", RequestAttendanceFormat.time(value))
    }

    /** A 12-hour clock remains a 12-hour clock, including its AM/PM suffix. */
    @Test
    fun twelveHourClockIsNotConverted() {
        assertEquals("12:32 PM", RequestAttendanceFormat.time("2026-11-05 12:32 PM"))
    }

    /** A 24-hour clock is not converted to AM/PM. */
    @Test
    fun twentyFourHourClockIsNotConverted() {
        assertEquals("21:32", RequestAttendanceFormat.time("2026-11-05 21:32"))
    }

    /** ISO clocks retain their hour/minute without timezone conversion. */
    @Test
    fun isoTimestampPartsRemainUnchanged() {
        val value = "2026-11-05T09:30:00+05:00"
        assertEquals("2026-11-05", RequestAttendanceFormat.date(value))
        assertEquals("09:30", RequestAttendanceFormat.time(value))
    }

    /** Clock-only data does not become a duplicate date or an invented calendar day. */
    @Test
    fun clockOnlyValueHasNoDate() {
        assertEquals("--", RequestAttendanceFormat.date("12:32 PM"))
        assertEquals("12:32 PM", RequestAttendanceFormat.time("12:32 PM"))
    }

    /** Date-only data does not acquire a synthetic midnight time. */
    @Test
    fun dateOnlyValueHasNoTime() {
        assertEquals("2026-11-05", RequestAttendanceFormat.date("2026-11-05"))
        assertEquals("--", RequestAttendanceFormat.time("2026-11-05"))
    }

    /** Empty and non-timestamp data use placeholders rather than appearing in the wrong label. */
    @Test
    fun missingValuesUsePlaceholders() {
        listOf(null, "", "--", "invalid").forEach {
            assertEquals("--", RequestAttendanceFormat.date(it))
            assertEquals("--", RequestAttendanceFormat.time(it))
        }
    }

    /** Submitted on uses the agreed date-plus-minute display instead of localized month names. */
    @Test
    fun submittedOnUsesDateAndMinutes() {
        assertEquals("2026-11-05 12:32", RequestAttendanceFormat.submittedOn("2026-11-05 12:32"))
        assertEquals("2026-11-05 12:32", RequestAttendanceFormat.submittedOn("2026-11-05 12:32:45.123456"))
    }

    /** Submitted on preserves a received AM/PM suffix and does not shift an offset clock. */
    @Test
    fun submittedOnDoesNotConvertClock() {
        assertEquals("2026-11-05 12:32 PM", RequestAttendanceFormat.submittedOn("2026-11-05 12:32:00 PM"))
        assertEquals("2026-11-05 09:30", RequestAttendanceFormat.submittedOn("2026-11-05T09:30:00+05:00"))
    }

    /** Missing creation values cannot invent a submission date or midnight time. */
    @Test
    fun missingCreationUsesPlaceholder() {
        assertEquals("--", RequestAttendanceFormat.submittedOn(null))
        assertEquals("--", RequestAttendanceFormat.submittedOn("12:32"))
        assertEquals("2026-11-05", RequestAttendanceFormat.submittedOn("2026-11-05"))
    }
}
