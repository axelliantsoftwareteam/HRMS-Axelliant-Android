package com.axelliant.hris.repos

import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.network.ApiResult

/** Home data boundary: reads can be reused, mutations always reach the server once. */
interface HomeSource {
    fun cachedDashboard(): DashboardResponse?
    fun dashboardIsFresh(): Boolean
    fun cachedTeam(): TodayTeamResponse?
    fun teamIsFresh(): Boolean
    suspend fun dashboard(force: Boolean = false): ApiResult<DashboardResponse>
    suspend fun team(): ApiResult<TodayTeamResponse>
    suspend fun punch(body: CheckInRequest): ApiResult<CheckInResponse>
    suspend fun employees(filter: String?): ApiResult<EmployProfileListModel>
}
