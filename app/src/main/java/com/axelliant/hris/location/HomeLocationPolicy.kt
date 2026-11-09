package com.axelliant.hris.location

/** Accepts monotonic GPS timestamps at most two minutes old, rejecting future/unknown fixes. */
internal fun isRecentAttendanceFix(nowNanos: Long, fixNanos: Long): Boolean =
    fixNanos > 0L && nowNanos - fixNanos in 0L..120_000_000_000L

/** Allows Android permission prompts before the first request or after a recoverable denial. */
internal fun canRequestHomeLocationPermission(asked: Boolean, rationale: Boolean): Boolean = !asked || rationale
