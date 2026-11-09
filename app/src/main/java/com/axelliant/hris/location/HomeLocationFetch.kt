package com.axelliant.hris.location

/** Tracks an explicit location refresh without accepting a fix from before the tap. */
internal class HomeLocationFetch {
    var active: Boolean = false
        private set
    private var startedNanos: Long? = null

    /** Starts one request; returns false when another request is already active. */
    fun begin(): Boolean {
        if (active) return false
        active = true
        return true
    }

    /** Starts the fix timeout after prerequisites, once per request; returns whether it started. */
    fun startFetching(nowNanos: Long): Boolean {
        if (!active || startedNanos != null) return false
        startedNanos = nowNanos
        return true
    }

    /** Returns whether [fixNanos] is a recent fix produced during this active fetch. */
    fun accepts(nowNanos: Long, fixNanos: Long): Boolean =
        active && startedNanos?.let { fixNanos >= it && isRecentAttendanceFix(nowNanos, fixNanos) } == true

    /** Ends or cancels a request, allowing another tap to start fresh. */
    fun finish() { active = false; startedNanos = null }
}
