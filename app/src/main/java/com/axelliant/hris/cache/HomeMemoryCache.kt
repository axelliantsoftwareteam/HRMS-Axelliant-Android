package com.axelliant.hris.cache

import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.SessionVersion
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Memory-only Home snapshots, isolated by login, date and timezone. */
object HomeMemoryCache {
    val dashboard = SessionValueCache<DashboardResponse>(30_000L)
    val team = SessionValueCache<TodayTeamResponse>(60_000L)

    fun key(): String? = AppConst.TOKEN?.takeIf { it.isNotBlank() }?.let {
        "${SessionVersion.current}|$it|${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}|${TimeZone.getDefault().id}"
    }

    fun attendanceChanged() { dashboard.clear(); team.clear() }
    fun clear() { SessionVersion.advance(); attendanceChanged() }
}
