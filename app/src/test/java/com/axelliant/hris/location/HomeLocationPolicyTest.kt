package com.axelliant.hris.location

import com.axelliant.hris.location.canRequestHomeLocationPermission
import com.axelliant.hris.location.isRecentAttendanceFix
import org.junit.Assert.*
import org.junit.Test

/** Guards first-request/permanent-denial routing and attendance fix freshness boundaries. */
class HomeLocationPolicyTest {
    @Test
    fun nativePromptIsAllowedUntilPermissionIsPermanentlyDenied() {
        assertTrue(canRequestHomeLocationPermission(false, false))
        assertTrue(canRequestHomeLocationPermission(false, true))
        assertTrue(canRequestHomeLocationPermission(true, true))
        assertFalse(canRequestHomeLocationPermission(true, false))
    }

    @Test
    fun onlyRecentNonFutureFixesCanAuthorizeAttendance() {
        val now = 200_000_000_000L
        assertTrue(isRecentAttendanceFix(now, now))
        assertTrue(isRecentAttendanceFix(now, now - 120_000_000_000L))
        assertFalse(isRecentAttendanceFix(now, now - 120_000_000_001L))
        assertFalse(isRecentAttendanceFix(now, now + 1L))
        assertFalse(isRecentAttendanceFix(now, 0L))
    }
}
