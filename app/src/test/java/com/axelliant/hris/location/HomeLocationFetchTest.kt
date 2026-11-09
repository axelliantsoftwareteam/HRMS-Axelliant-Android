package com.axelliant.hris.location

import com.axelliant.hris.location.HomeLocationFetch
import org.junit.Assert.*
import org.junit.Test

/** Explicit location refresh must share taps, reject cached fixes, and reset after cancellation. */
class HomeLocationFetchTest {
    @Test
    fun repeatedTapsAndPrerequisiteCallbacksDoNotRestartFetch() {
        val fetch = HomeLocationFetch()
        assertTrue(fetch.begin())
        assertFalse(fetch.begin())
        assertTrue(fetch.startFetching(100L))
        assertFalse(fetch.startFetching(200L))
        assertTrue(fetch.accepts(200L, 100L))
    }

    @Test
    fun acceptsOnlyRecentFixProducedAfterPrerequisites() {
        val fetch = HomeLocationFetch()
        assertFalse(fetch.startFetching(100L))
        assertFalse(fetch.accepts(100L, 100L))
        fetch.begin()
        assertFalse(fetch.accepts(100L, 100L))
        fetch.startFetching(100L)
        assertFalse(fetch.accepts(200L, 99L))
        assertFalse(fetch.accepts(200L, 201L))
        assertFalse(fetch.accepts(120_000_000_201L, 200L))
        assertTrue(fetch.accepts(200L, 200L))
    }

    @Test
    fun timeoutOrCancellationAllowsNewRequestWithoutOldStartTime() {
        val fetch = HomeLocationFetch()
        fetch.begin()
        fetch.startFetching(200L)
        fetch.finish()
        assertFalse(fetch.active)
        assertFalse(fetch.accepts(300L, 300L))
        assertTrue(fetch.begin())
        assertTrue(fetch.startFetching(400L))
        assertFalse(fetch.accepts(500L, 300L))
        assertTrue(fetch.accepts(500L, 400L))
    }
}
