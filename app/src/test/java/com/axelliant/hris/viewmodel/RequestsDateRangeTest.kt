package com.axelliant.hris.viewmodel

import com.axelliant.hris.viewmodel.RequestsOverviewViewModel.Period
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

/** Verifies request periods and UTC picker conversion across calendar and timezone boundaries. */
class RequestsDateRangeTest {
    private val previousZone = TimeZone.getDefault()

    /** Fixes the timezone for deterministic preset dates. */
    @Before
    fun setUp() { TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }

    /** Restores the host timezone for unrelated tests. */
    @After
    fun tearDown() { TimeZone.setDefault(previousZone) }

    /** Last seven days includes today and crosses into the prior year correctly. */
    @Test
    fun weekIncludesSevenDaysAcrossNewYear() {
        assertEquals("2025-12-26" to "2026-01-01",
            RequestsDateRange.preset(Period.LAST_7_DAYS, day(2026, 1, 1)))
    }

    /** Current Month retains the existing first-through-last-day semantics in leap years. */
    @Test
    fun leapFebruaryIncludesTwentyNinth() {
        assertEquals("2024-02-01" to "2024-02-29",
            RequestsDateRange.preset(Period.THIS_MONTH, day(2024, 2, 10)))
    }

    /** Non-leap February does not request a nonexistent twenty-ninth. */
    @Test
    fun ordinaryFebruaryEndsOnTwentyEighth() {
        assertEquals("2025-02-01" to "2025-02-28",
            RequestsDateRange.preset(Period.THIS_MONTH, day(2025, 2, 10)))
    }

    /** Historical custom ranges can span more than one month. */
    @Test
    fun customRangeAllowsOlderMonths() {
        assertEquals("2026-09-01" to "2026-10-31", RequestsDateRange.selected(
            utc("2026-09-01T00:00:00Z"), utc("2026-10-31T00:00:00Z")))
    }

    /** A same-day request range remains valid. */
    @Test
    fun customRangeAllowsSameDay() {
        val date = utc("2026-10-01T00:00:00Z")
        assertEquals("2026-10-01" to "2026-10-01", RequestsDateRange.selected(date, date))
    }

    /** Formatting picker dates must not move them to the preceding local calendar day. */
    @Test
    fun customRangeIgnoresDeviceTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
        val date = utc("2026-10-01T00:00:00Z")
        assertEquals("2026-10-01" to "2026-10-01", RequestsDateRange.selected(date, date))
        assertEquals(date, RequestsDateRange.utcMillis("2026-10-01"))
    }

    /** Reversed bounds fail before an API range can be applied. */
    @Test(expected = IllegalArgumentException::class)
    fun reversedRangeIsRejected() {
        RequestsDateRange.selected(utc("2026-10-02T00:00:00Z"), utc("2026-10-01T00:00:00Z"))
    }

    /** Invalid restored date strings are not passed into the calendar picker. */
    @Test
    fun invalidPickerDatesAreRejected() {
        listOf("", "invalid", "2026-02-30", "2026-10-01 trailing").forEach {
            assertNull(it, RequestsDateRange.utcMillis(it))
        }
    }

    /** Computing presets must not mutate the caller's current calendar. */
    @Test
    fun presetDoesNotMutateClock() {
        val today = day(2026, 11, 5)
        val original = today.timeInMillis
        RequestsDateRange.preset(Period.THIS_MONTH, today)
        RequestsDateRange.preset(Period.LAST_7_DAYS, today)
        assertEquals(original, today.timeInMillis)
    }

    private fun day(year: Int, month: Int, date: Int) = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, date, 12, 0, 0)
    }
    private fun utc(value: String) = Instant.parse(value).toEpochMilli()
}
