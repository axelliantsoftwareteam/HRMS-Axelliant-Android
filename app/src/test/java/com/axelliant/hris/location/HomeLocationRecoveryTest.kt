package com.axelliant.hris.location

import com.axelliant.hris.location.HomeLocationFetch
import com.axelliant.hris.location.canRequestHomeLocationPermission
import org.junit.Assert.*
import org.junit.Test

/** Checks location retry and freshness state without Android permission dialogs or GPS hardware. */
class HomeLocationRecoveryTest {
    /** A retry after cancellation must reject a delayed fix belonging to the previous attempt. */
    @Test
    fun retryRejectsPreviousAttemptFixAndAcceptsNewFix() {
        val fetch = HomeLocationFetch()
        assertTrue(fetch.begin())
        assertTrue(fetch.startFetching(1_000L))
        fetch.finish()

        assertTrue(fetch.begin())
        assertTrue(fetch.startFetching(3_000L))
        assertFalse(fetch.accepts(4_000L, 2_000L))
        assertTrue(fetch.accepts(4_000L, 3_500L))
        fetch.finish()
        assertFalse(fetch.active)
        assertFalse(fetch.accepts(4_000L, 3_500L))
    }

    /** Permission prerequisites alone never authorize a fix before actual fetching begins. */
    @Test
    fun permissionRecoveryStillRequiresNewFetchBeforeAcceptingLocation() {
        val fetch = HomeLocationFetch()
        assertTrue(canRequestHomeLocationPermission(asked = false, rationale = false))
        fetch.begin()
        assertFalse(fetch.accepts(2_000L, 2_000L))
        fetch.finish()
        assertFalse(canRequestHomeLocationPermission(asked = true, rationale = false))

        assertTrue(fetch.begin())
        assertFalse(fetch.accepts(3_000L, 3_000L))
        assertTrue(fetch.startFetching(4_000L))
        assertFalse(fetch.accepts(5_000L, 3_000L))
        assertTrue(fetch.accepts(5_000L, 4_500L))
    }

    /** A fix expires exactly after the allowed two-minute boundary, including during a long fetch. */
    @Test
    fun fixAtFreshnessLimitIsAcceptedButOneNanosecondOlderIsRejected() {
        val fetch = HomeLocationFetch()
        val fix = 10_000_000_000L
        fetch.begin()
        fetch.startFetching(fix)

        assertTrue(fetch.accepts(fix + 120_000_000_000L, fix))
        assertFalse(fetch.accepts(fix + 120_000_000_001L, fix))
        fetch.finish()
        assertFalse(fetch.active)
    }
}
