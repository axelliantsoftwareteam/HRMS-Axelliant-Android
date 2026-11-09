package com.axelliant.hris.viewmodel

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.ViewModelStore
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.repos.HomeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var source: FakeHome
    private lateinit var model: HomeViewModel
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        source = FakeHome()
        model = HomeViewModel(source)
        store.put("home", model)
    }

    @After fun cleanup() {
        store.clear()
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    @Test fun freshCacheIsReplayedWithoutLoadingOrFetch() = runTest(dispatcher) {
        source.fresh = true
        model.getDashboardInformation()
        advanceUntilIdle()
        assertSame(source.snapshot, model.dashboardResponse.value)
        assertEquals(0, source.dashboardCalls)
        assertEquals(false, model.refreshing.value)
        assertNull(model.isLoading.value)
    }

    @Test fun repeatedRefreshesShareActiveDashboardRequest() = runTest(dispatcher) {
        source.dashboardGate = CompletableDeferred()
        model.getDashboardInformation(); model.getDashboardInformation(true)
        runCurrent()
        assertEquals(1, source.dashboardCalls)
        assertSame(source.snapshot, model.dashboardResponse.value)
        assertEquals(true, model.refreshing.value)
        assertNull(model.isLoading.value)
        source.dashboardGate!!.complete(ApiResult.Success(source.snapshot))
        advanceUntilIdle()
        assertEquals(false, model.refreshing.value)
    }

    @Test fun refreshFailureKeepsLastGoodContent() = runTest(dispatcher) {
        source.dashboardGate = CompletableDeferred(ApiResult.Failure("offline"))
        model.getDashboardInformation(); advanceUntilIdle()
        assertSame(source.snapshot, model.dashboardResponse.value)
        assertEquals("offline", model.homeError.value?.peekContent())
    }

    @Test fun expiredAttendanceRefreshesWithoutReplayingPunch() = runTest(dispatcher) {
        model.postCheckIn(CheckInRequest())
        advanceUntilIdle()
        assertEquals(0, source.punchCalls)
        assertEquals(1, source.dashboardCalls)
        assertNotNull(model.homeError.value)
    }

    @Test fun doubleTapSubmitsOnceAndSuccessForcesDashboardRefresh() = runTest(dispatcher) {
        source.fresh = true
        source.punchGate = CompletableDeferred()
        model.postCheckIn(CheckInRequest()); model.postCheckIn(CheckInRequest())
        runCurrent()
        assertEquals(1, source.punchCalls)
        source.punchGate!!.complete(ApiResult.Success(CheckInResponse(meta = Meta("OK", true))))
        advanceUntilIdle()
        assertEquals(1, source.dashboardCalls)
        assertTrue(source.forced)
        assertEquals(1, source.punchCalls)
        assertNotNull(model.checkInResponse.value)
        assertEquals(false, model.isLoading.value?.peekContent())
    }

    @Test fun failedPunchIsNotRetriedAndDoesNotRefreshAttendance() = runTest(dispatcher) {
        source.fresh = true
        source.punchGate = CompletableDeferred(ApiResult.Failure("timeout"))
        model.postCheckIn(CheckInRequest()); advanceUntilIdle()
        assertEquals(1, source.punchCalls)
        assertEquals(0, source.dashboardCalls)
        assertEquals("timeout", model.homeError.value?.peekContent())
    }

    @Test fun clearingViewModelCancelsPendingRead() = runTest(dispatcher) {
        source.dashboardGate = CompletableDeferred()
        model.getDashboardInformation(); runCurrent()
        store.clear(); advanceUntilIdle()
        source.dashboardGate!!.complete(ApiResult.Failure("late"))
        advanceUntilIdle()
        assertNull(model.homeError.value)
        assertEquals(false, model.refreshing.value)
    }

    private class FakeHome : HomeSource {
        val snapshot = DashboardResponse(meta = Meta("OK", true))
        var fresh = false
        var dashboardCalls = 0
        var punchCalls = 0
        var forced = false
        var dashboardGate: CompletableDeferred<ApiResult<DashboardResponse>>? = null
        var punchGate: CompletableDeferred<ApiResult<CheckInResponse>>? = null
        override fun cachedDashboard() = snapshot
        override fun dashboardIsFresh() = fresh
        override fun cachedTeam(): TodayTeamResponse? = null
        override fun teamIsFresh() = false
        override suspend fun dashboard(force: Boolean): ApiResult<DashboardResponse> {
            dashboardCalls++; forced = force
            return dashboardGate?.await() ?: ApiResult.Success(snapshot).also { fresh = true }
        }
        override suspend fun punch(body: CheckInRequest): ApiResult<CheckInResponse> {
            punchCalls++
            return (punchGate?.await() ?: ApiResult.Success(CheckInResponse(meta = Meta("OK", true))))
                .also { if (it is ApiResult.Success) fresh = false }
        }
        override suspend fun team(): ApiResult<TodayTeamResponse> = ApiResult.Success(TodayTeamResponse(meta = Meta("OK", true)))
        override suspend fun employees(filter: String?): ApiResult<EmployProfileListModel> =
            ApiResult.Success(EmployProfileListModel(meta = Meta("OK", true)))
    }
}
