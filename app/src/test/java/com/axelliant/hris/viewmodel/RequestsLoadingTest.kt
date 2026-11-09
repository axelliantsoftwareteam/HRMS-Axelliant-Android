package com.axelliant.hris.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.requests.*
import com.axelliant.hris.network.ApiResult
import java.lang.reflect.Type
import java.time.Instant
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Call

class RequestsLoadingTest : ListScreenTestSupport() {
    private val network = ListNetworkHarness()
    private fun model(state: SavedStateHandle = SavedStateHandle()) = retain(object : RequestsOverviewViewModel(network.api, state,
        { sources, previous -> sources.joinToString { it.name } + "; previous=$previous" }) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun succeed(calls: List<TestListCall>, prefix: String = "") {
        calls.forEach { call ->
            val data = when (call.name) {
                "postLeaveRequestsMap" -> LeaveRequestsPayload(listOf(LeaveRequestItem(name = "${prefix}L", from_date = "2026-11-01")))
                "postCheckinsMap" -> CheckinRequestsPayload(listOf(CheckinRequestItem(name = "${prefix}C", time = "2026-11-02 09:30")))
                else -> ExpenseClaimsPayload(expenses = listOf(ExpenseClaim(name = "${prefix}E", posting_date = "2026-11-03")))
            }
            network.complete(call, ApiResult.Success(data))
        }
    }

    @Test fun batchWaitsForAllThreeAndPublishesOneSortedSnapshot() {
        val vm = model()
        vm.load(ListLoadKind.INITIAL)
        val calls = network.calls.toList()
        succeed(calls.take(2))
        assertNull(vm.rows.value)
        assertTrue(vm.listLoading().value!!.initialLoading)
        succeed(calls.takeLast(1))
        assertEquals(listOf("E", "C", "L"), vm.rows.value!!.map { it.reference })
        assertFalse(vm.listLoading().value!!.isLoading)
        assertEquals(3, vm.listLoading().value!!.itemCount)
    }

    @Test fun partialFirstLoadReportsMissingSourceWithoutPublishingIncompleteData() {
        val vm = model()
        vm.load()
        succeed(network.calls.filter { it.name != "postCheckinsMap" })
        network.complete(network.last("postCheckinsMap"), ApiResult.Failure("network failed"))
        assertNull(vm.rows.value)
        assertFalse(vm.listLoading().value!!.hasLoaded)
        assertTrue(vm.listLoading().value!!.error!!.contains("ATTENDANCE"))
        assertTrue(vm.listLoading().value!!.error!!.contains("previous=false"))
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun partialRefreshRetainsEntirePreviousSnapshotAndReportsFailures() {
        val vm = model()
        vm.load(); succeed(network.calls.toList())
        val original = vm.rows.value
        vm.load(ListLoadKind.PULL_REFRESH)
        val refresh = network.calls.takeLast(3)
        assertSame(original, vm.rows.value)
        assertTrue(vm.listLoading().value!!.pullRefreshing)
        succeed(refresh.filter { it.name == "postCheckinsMap" }, "new")
        refresh.filter { it.name != "postCheckinsMap" }.forEach { network.complete(it, ApiResult.Failure("offline")) }
        assertSame(original, vm.rows.value)
        val error = vm.listLoading().value!!.error!!
        assertTrue(error.contains("LEAVE")); assertTrue(error.contains("EXPENSE"))
        assertTrue(error.contains("previous=true"))
        assertFalse(vm.listLoading().value!!.pullRefreshing)
    }

    @Test fun oldDateBatchCannotOverwriteRowsOrStopCurrentSpinner() {
        val vm = model()
        vm.load()
        val old = network.calls.toList()
        vm.setCustomRange(utc("2026-10-01"), utc("2026-10-31"))
        vm.load(ListLoadKind.FILTER)
        val current = network.calls.takeLast(3)
        assertTrue(old.all { it.cancelled })
        old.forEach { network.complete(it, ApiResult.Failure("old")) }
        assertTrue(vm.listLoading().value!!.isLoading)
        assertNull(vm.listLoading().value!!.error)
        succeed(current, "current")
        succeed(old, "old")
        assertTrue(vm.rows.value!!.all { it.reference.startsWith("current") })
        assertNull(vm.listLoading().value!!.error)
        assertEquals("2026-10-01" to "2026-10-31", vm.displayedRange)
    }

    @Test fun sameInFlightRangeIsNotFetchedTwiceButExplicitRefreshReplacesIt() {
        val vm = model()
        vm.load(); vm.load()
        assertEquals(3, network.calls.size)
        val old = network.calls.toList()
        vm.load(ListLoadKind.PULL_REFRESH, force = true)
        assertEquals(6, network.calls.size)
        assertTrue(old.all { it.cancelled })
    }

    @Test fun customSelectionRestoresAndAppliesSameDatesToEveryApi() {
        val state = SavedStateHandle()
        val vm = model(state)
        vm.setCustomRange(utc("2026-09-01"), utc("2026-10-31"))
        val restored = model(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        assertEquals(RequestsOverviewViewModel.Period.CUSTOM, restored.period)
        restored.load()
        network.calls.forEach { call ->
            if (call.name == "callMyExpenseDetail") {
                val body = call.args[1] as AttRequest
                assertEquals("2026-09-01", body.start_date)
                assertEquals("2026-10-31", body.end_date)
            } else {
                val body = call.args[1] as Map<*, *>
                assertEquals("2026-09-01", body["start_date"])
                assertEquals("2026-10-31", body["end_date"])
            }
        }
    }

    @Test fun loadedRowsReplayAfterViewObserverReattaches() {
        val vm = model()
        vm.load(); succeed(network.calls.toList())
        var rendered: List<RequestRow>? = null
        val observer = Observer<List<RequestRow>?> { rendered = it }
        vm.rows.observeForever(observer); vm.rows.removeObserver(observer)
        rendered = null
        vm.rows.observeForever(observer)
        assertEquals(3, rendered!!.size)
        assertEquals(3, network.calls.size)
        vm.rows.removeObserver(observer)
    }

    @Test fun emptyIsOnlyPublishedAfterAllSourcesSucceed() {
        val vm = model()
        vm.load()
        network.complete(network.last("postLeaveRequestsMap"), ApiResult.Success(LeaveRequestsPayload()))
        network.complete(network.last("postCheckinsMap"), ApiResult.Success(CheckinRequestsPayload()))
        assertFalse(vm.listLoading().value!!.hasLoaded)
        network.complete(network.last("callMyExpenseDetail"), ApiResult.Success(ExpenseClaimsPayload()))
        assertTrue(vm.listLoading().value!!.isEmpty)
        assertEquals(emptyList<RequestRow>(), vm.rows.value)
    }

    @Test fun clearingModelCancelsEveryMemberAndIgnoresLateCompletions() {
        val vm = model()
        vm.load()
        clearModels()
        assertTrue(network.calls.all { it.cancelled })
        succeed(network.calls.toList())
        assertNull(vm.rows.value)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    private fun utc(day: String) = Instant.parse("${day}T00:00:00Z").toEpochMilli()
}
