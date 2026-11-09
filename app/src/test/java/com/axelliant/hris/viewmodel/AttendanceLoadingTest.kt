package com.axelliant.hris.viewmodel

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.leaveatt.AttendanceDay
import com.axelliant.hris.model.leaveatt.AttendancePayload
import com.axelliant.hris.model.leaveatt.ShiftDetail
import com.axelliant.hris.model.leaveatt.WeekStatsPayload
import com.axelliant.hris.network.ApiResult
import java.lang.reflect.Type
import java.time.Instant
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Call

class AttendanceLoadingTest : ListScreenTestSupport() {
    private val network = ListNetworkHarness()
    private fun model(state: SavedStateHandle = SavedStateHandle()) = retain(object : AttendanceViewModel(network.api, state) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    }).also { it.initialize("EMP-1") }
    private fun payload(day: String) = AttendancePayload(attendance_data = listOf(AttendanceDay(date = day)))

    @Test fun newerFilterCancelsOldReadAndIgnoresItsSuccessAndError() {
        val vm = model()
        vm.ensureLoaded()
        val old = network.last("callAttendanceDetail")
        vm.selectFilter("Absent")
        val current = network.last("callAttendanceDetail")
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Success(payload("2026-11-01")))
        assertNull(vm.attendance.value)
        assertTrue(vm.listLoading().value!!.initialLoading)
        network.complete(current, ApiResult.Success(payload("2026-11-02")))
        network.complete(old, ApiResult.Failure("obsolete error"))
        assertEquals("2026-11-02", vm.attendance.value!!.attendance_data!!.single().date)
        assertNull(vm.listLoading().value!!.error)
        assertEquals("Absent", (current.args[1] as AttRequest).filters)
    }

    @Test fun selectionsAndCustomDatesSurviveSavedStateRestoration() {
        val state = SavedStateHandle()
        val vm = model(state)
        vm.setCustomRange(utc("2026-10-01"), utc("2026-10-31"))
        vm.selectFilter("Late")
        val restored = model(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        assertEquals(vm.selection.value, restored.selection.value)
        restored.ensureLoaded()
        val body = network.last("callAttendanceDetail").args[1] as AttRequest
        assertEquals("2026-10-01", body.start_date)
        assertEquals("2026-10-31", body.end_date)
        assertEquals(listOf("EMP-1"), body.employee_list)
    }

    @Test fun viewReattachmentReplaysContentWithoutResettingSelectionOrFetchingAgain() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        network.complete(network.last("callAttendanceWeekStats"), ApiResult.Success(WeekStatsPayload(ShiftDetail(name = "Day"))))
        val before = network.calls.size
        var rendered: AttendancePayload? = null
        val observer = Observer<AttendancePayload?> { rendered = it }
        vm.attendance.observeForever(observer)
        vm.attendance.removeObserver(observer)
        vm.ensureLoaded()
        vm.attendance.observeForever(observer)
        assertNotNull(rendered)
        assertEquals(before, network.calls.size)
        vm.attendance.removeObserver(observer)
    }

    @Test fun optionalShiftDoesNotEndOrBlockAttendanceLoading() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceWeekStats"), ApiResult.Failure("optional failed"))
        assertTrue(vm.listLoading().value!!.initialLoading)
        assertNull(vm.listLoading().value!!.error)
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun attendanceCompletionDoesNotWaitForShift() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        assertFalse(vm.listLoading().value!!.isLoading)
        network.complete(network.last("callAttendanceWeekStats"), ApiResult.Success(WeekStatsPayload(ShiftDetail(name = "Late shift"))))
        assertEquals("Late shift", vm.shift.value!!.name)
    }

    @Test fun failedRefreshRetainsDaysAndSelectedQuery() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        val original = vm.attendance.value
        vm.selectPeriod(AttendanceViewModel.Period.MONTH)
        assertSame(original, vm.attendance.value)
        assertTrue(vm.listLoading().value!!.inlineRefreshing)
        network.complete(network.last("callAttendanceDetail"), ApiResult.Failure("offline"))
        assertSame(original, vm.attendance.value)
        assertNotEquals(vm.displayedSelection, vm.selection.value)
        assertEquals("offline", vm.listLoading().value!!.error)
    }

    @Test fun pullSpinnerStopsWithAttendanceEvenWhenShiftIsPending() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        vm.loadShift(force = true)
        vm.reload(ListLoadKind.PULL_REFRESH)
        assertTrue(vm.listLoading().value!!.pullRefreshing)
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(AttendancePayload()))
        assertFalse(vm.listLoading().value!!.pullRefreshing)
        assertTrue(vm.listLoading().value!!.isEmpty)
    }

    @Test fun repeatedEnsureWhileLoadingDoesNotDuplicateCalls() {
        val vm = model()
        vm.ensureLoaded(); vm.ensureLoaded()
        assertEquals(2, network.calls.size)
    }

    @Test fun recreationKeepsInitialErrorUntilExplicitRetry() {
        val vm = model()
        vm.ensureLoaded()
        network.complete(network.last("callAttendanceDetail"), ApiResult.Failure("offline"))
        vm.ensureLoaded()
        assertEquals(1, network.calls.count { it.name == "callAttendanceDetail" })
        assertEquals("offline", vm.listLoading().value!!.error)
        vm.reload()
        assertNull(vm.listLoading().value!!.error)
    }

    @Test fun clearCancelsShiftAndAttendanceAndRejectsLateCallbacks() {
        val vm = model()
        vm.ensureLoaded()
        clearModels()
        assertTrue(network.calls.all { it.cancelled })
        network.complete(network.last("callAttendanceDetail"), ApiResult.Success(payload("2026-11-01")))
        network.complete(network.last("callAttendanceWeekStats"), ApiResult.Success(WeekStatsPayload(ShiftDetail(name = "Late"))))
        assertNull(vm.attendance.value)
        assertNull(vm.shift.value)
    }

    private fun utc(day: String) = Instant.parse("${day}T00:00:00Z").toEpochMilli()
}
