package com.axelliant.hris.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class WsFormatTest {

    init {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `date formats day month year`() {
        assertEquals("30 Sep 2026", WsFormat.date("2026-09-30"))
        assertEquals("", WsFormat.date(""))
        assertEquals("", WsFormat.date("garbage"))
    }

    @Test
    fun `date range collapses equal ends`() {
        assertEquals("5 Sep 2026 – 7 Sep 2026", WsFormat.dateRange("2026-09-05", "2026-09-07"))
        assertEquals("5 Sep 2026", WsFormat.dateRange("2026-09-05", "2026-09-05"))
        assertEquals("7 Sep 2026", WsFormat.dateRange("", "2026-09-07"))
    }

    @Test
    fun `days and hours read naturally`() {
        assertEquals("1 day", WsFormat.days(1.0))
        assertEquals("2.5 days", WsFormat.days(2.5))
        assertEquals("3 days", WsFormat.days(3.0))
        assertEquals("", WsFormat.days(0.0))
        assertEquals("8.5 hrs", WsFormat.hours(8.5))
        assertEquals("", WsFormat.hours(null))
    }

    @Test
    fun `numbers drop a trailing zero`() {
        assertEquals("7", WsFormat.number(7.0))
        assertEquals("1.5", WsFormat.number(1.5))
        assertEquals("0", WsFormat.number(null))
    }

    @Test
    fun `reversed day and display to api round trip`() {
        assertEquals("30-09-2026", WsFormat.reversedDay("2026-09-30"))
        assertEquals("2026-09-30", WsFormat.displayToApi("30-09-2026"))
        assertEquals("", WsFormat.displayToApi("nope"))
    }

    @Test
    fun `media urls resolve against the hris host`() {
        assertEquals("https://hris.axelliant.com/files/a.png", WsFormat.mediaUrl("/files/a.png"))
        assertEquals("https://cdn.example/a.png", WsFormat.mediaUrl("https://cdn.example/a.png"))
        assertEquals("", WsFormat.mediaUrl(null))
    }

    @Test
    fun `initials use the first two words`() {
        assertEquals("UA", WsFormat.initials("Muhammad Umer Azhar".split(" ").drop(1).joinToString(" ")))
        assertEquals("MU", WsFormat.initials("Muhammad Umer Azhar"))
        assertEquals("?", WsFormat.initials(""))
    }

    @Test
    fun `parse understands hris timestamps`() {
        assertNotNull(WsFormat.parse("2026-09-30 18:10:45.123456"))
        assertNotNull(WsFormat.parse("2026-09-30 18:10:45"))
        assertNotNull(WsFormat.parse("2026-09-30T18:10:45+05:00"))
        assertNull(WsFormat.parse(null))
    }

    @Test
    fun `leave and draft statuses read as pending`() {
        assertEquals("Pending", WsStatus.displayLeave("Open"))
        assertEquals("Pending", WsStatus.displayLeave(null))
        assertEquals("Approved", WsStatus.displayLeave("Approved"))
        assertEquals("Pending", WsStatus.displayDraft("Draft"))
    }
}
