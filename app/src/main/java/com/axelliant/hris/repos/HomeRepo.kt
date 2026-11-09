package com.axelliant.hris.repos

import com.axelliant.hris.cache.HomeMemoryCache
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.post.TeamListRequest
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import retrofit2.Call
import kotlin.coroutines.resume

/** Cancellable reads with session caches. Attendance writes are never cached or automatically retried. */
class HomeRepo(private val api: ApiInterface) : HomeSource {
    override fun cachedDashboard() = HomeMemoryCache.key()?.let(HomeMemoryCache.dashboard::peek)
    override fun dashboardIsFresh() = HomeMemoryCache.key()?.let(HomeMemoryCache.dashboard::isFresh) == true
    override fun cachedTeam() = HomeMemoryCache.key()?.let(HomeMemoryCache.team::peek)
    override fun teamIsFresh() = HomeMemoryCache.key()?.let(HomeMemoryCache.team::isFresh) == true

    override suspend fun dashboard(force: Boolean): ApiResult<DashboardResponse> {
        val key = HomeMemoryCache.key() ?: return noSession()
        val auth = AppConst.authHeader()
        return HomeMemoryCache.dashboard.load(key, { HomeMemoryCache.key() == key }, force) {
            await(api.callDashBoard(auth), DashboardResponse::class.java)
        }
    }

    override suspend fun team(): ApiResult<TodayTeamResponse> {
        val key = HomeMemoryCache.key() ?: return noSession()
        val auth = AppConst.authHeader()
        return HomeMemoryCache.team.load(key, { HomeMemoryCache.key() == key }) {
            await(api.callTeamInfo(auth), TodayTeamResponse::class.java)
        }
    }

    override suspend fun punch(body: CheckInRequest): ApiResult<CheckInResponse> {
        val key = HomeMemoryCache.key() ?: return noSession()
        val result = await(api.callCheckIn(AppConst.authHeader(), body), CheckInResponse::class.java)
        if (HomeMemoryCache.key() != key) return noSession()
        if (result is ApiResult.Success) HomeMemoryCache.attendanceChanged()
        return result
    }

    override suspend fun employees(filter: String?): ApiResult<EmployProfileListModel> {
        val key = HomeMemoryCache.key() ?: return noSession()
        val result = await(api.callTodayTeamList(AppConst.authHeader(), TeamListRequest().apply { filters = filter }),
            EmployProfileListModel::class.java)
        return if (HomeMemoryCache.key() == key) result else noSession()
    }

    private fun noSession() = ApiResult.Failure("Session changed. Please sign in again.")

    private suspend fun <T> await(call: Call<ResponseBody>, type: Class<T>): ApiResult<T> =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueueResult<T>(type) { if (continuation.isActive) continuation.resume(it) }
        }
}
