package com.axelliant.hris.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.approvals.*
import com.axelliant.hris.model.leaveatt.*
import com.axelliant.hris.model.requests.*
import com.axelliant.hris.model.team.*
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.repos.ExpenseRepo
import com.axelliant.hris.utils.Validator
import java.lang.reflect.Type
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Call

class RemainingListLoadingTest : ListScreenTestSupport() {
    private val network = ListNetworkHarness()
    private fun expenses(state: SavedStateHandle = SavedStateHandle()) = retain(object : ExpenseViewModel(ExpenseRepo(network.api), Validator(), state) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun leaves() = retain(object : LeaveViewModel(network.api, { "incomplete; previous=$it" }) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun approvals() = retain(object : ApprovalsViewModel(network.api) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun team() = retain(object : TeamActivityViewModel(network.api, { "incomplete; previous=$it" }) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun calendar() = retain(object : TeamCalendarViewModel(network.api) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun organogram() = retain(object : OrganogramViewModel(network.api) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })

    @Test fun expenseFiltersCancelObsoleteReadsAndRetainLastGoodPayloadOnFailure() {
        val vm = expenses()
        vm.loadClaims("2026-11-01", "2026-11-09", "", ListLoadKind.INITIAL)
        val old = network.last("callMyExpenseDetail")
        vm.loadClaims("2026-10-01", "2026-10-31", "Draft", ListLoadKind.FILTER)
        val current = network.last("callMyExpenseDetail")
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Failure("stale"))
        assertNull(vm.listLoading("claims").value!!.error)
        val data = ExpenseClaimsPayload(expenses = listOf(ExpenseClaim(name = "E1")))
        network.complete(current, ApiResult.Success(data))
        vm.loadClaims("2026-10-01", "2026-10-31", "Draft", ListLoadKind.PULL_REFRESH)
        network.complete(network.last("callMyExpenseDetail"), ApiResult.Failure("offline"))
        assertSame(data, vm.claims.value)
        assertEquals("offline", vm.listLoading("claims").value!!.error)
        assertFalse(vm.listLoading("claims").value!!.pullRefreshing)
    }

    @Test fun expenseDeleteRetainsBlockingProgressSeparatelyFromReads() {
        val vm = expenses()
        vm.loadClaims("2026-11-01", "2026-11-09", "", ListLoadKind.INITIAL)
        vm.deleteClaim("E1")
        assertEquals(true, vm.blockingLoading.value)
        network.complete(network.last("callMyExpenseDetail"), ApiResult.Success(ExpenseClaimsPayload()))
        assertEquals(true, vm.blockingLoading.value)
        network.complete(network.last("deleteExpenseCall"), ApiResult.Success(StatusPayload()))
        assertEquals(false, vm.blockingLoading.value)
        assertNotNull(vm.deleteClaimResult.value)
    }

    @Test fun expenseSelectionPersistsThroughSavedState() {
        val state = SavedStateHandle()
        val vm = expenses(state)
        vm.selectedPeriod = "THIS_MONTH"; vm.selectedStatus = "Approved"
        val restored = expenses(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        assertEquals("THIS_MONTH", restored.selectedPeriod)
        assertEquals("Approved", restored.selectedStatus)
    }

    @Test fun expenseApprovalsFailureDoesNotClearExistingClaims() {
        val vm = expenses()
        vm.loadApprovals("2025-11-09", "2026-11-09", ListLoadKind.INITIAL)
        val data = ExpenseClaimsPayload(expenses = listOf(ExpenseClaim(name = "pending")))
        network.complete(network.last("callExpenseApproval"), ApiResult.Success(data))
        vm.loadApprovals("2025-11-09", "2026-11-09", ListLoadKind.PULL_REFRESH)
        network.complete(network.last("callExpenseApproval"), ApiResult.Failure("offline"))
        assertSame(data, vm.approvals.value)
        assertFalse(vm.listLoading("approvals").value!!.isLoading)
    }

    @Test fun leavesWaitForBothSourcesAndRetainSnapshotOnPartialRefresh() {
        val vm = leaves(); vm.load()
        network.complete(network.last("getLeaveTypesWithCount"), ApiResult.Success(LeaveQuotaPayload()))
        assertNull(vm.data.value)
        assertTrue(vm.listLoading().value!!.initialLoading)
        network.complete(network.last("callMyLeaveDetail"), ApiResult.Success(LeaveHistoryPayload(listOf(LeaveRecord(name = "L1")))))
        val old = vm.data.value
        vm.load(ListLoadKind.PULL_REFRESH)
        network.complete(network.last("getLeaveTypesWithCount"), ApiResult.Success(LeaveQuotaPayload()))
        network.complete(network.last("callMyLeaveDetail"), ApiResult.Failure("offline"))
        assertSame(old, vm.data.value)
        assertTrue(vm.listLoading().value!!.error!!.contains("previous=true"))
        assertFalse(vm.listLoading().value!!.pullRefreshing)
    }

    @Test fun replacedLeaveBatchCannotCompleteCurrentLoad() {
        val vm = leaves(); vm.load()
        val old = network.calls.toList()
        vm.load()
        assertTrue(old.all { it.cancelled })
        old.forEach { network.complete(it, ApiResult.Failure("stale")) }
        assertTrue(vm.listLoading().value!!.isLoading)
        assertNull(vm.listLoading().value!!.error)
    }

    @Test fun approvalsPageFailureKeepsPageAndSingleDecisionCannotRepeat() {
        val vm = approvals(); vm.load()
        val item = ApprovalItem(approval_type = "leave", reference_name = "L1")
        network.complete(network.last("getApprovals"), ApiResult.Success(ApprovalsPage(listOf(item), 21)))
        vm.movePage(1)
        val page = network.last("getApprovals")
        assertEquals(10, page.args[2])
        network.complete(page, ApiResult.Failure("offline"))
        assertEquals(0, vm.pageIndex)
        assertEquals(listOf(item), vm.items)
        vm.act(item, true); vm.act(item, true)
        assertEquals(1, network.calls.count { it.name == "takeApprovalAction" })
        assertTrue(vm.acting.value!!)
        assertFalse(vm.canGoForward)
        network.complete(network.last("takeApprovalAction"), ApiResult.Success(ApprovalActionResult("Done")))
        assertFalse(vm.acting.value!!)
        assertTrue(vm.listLoading().value!!.isLoading)
    }

    @Test fun emptyLastApprovalPageFallsBackWithoutResubmittingAnyAction() {
        val vm = approvals(); vm.load(1)
        network.complete(network.last("getApprovals"), ApiResult.Success(ApprovalsPage(emptyList(), 2)))
        assertEquals(0, network.last("getApprovals").args[2])
        assertTrue(vm.listLoading().value!!.isLoading)
        network.complete(network.last("getApprovals"), ApiResult.Success(ApprovalsPage(listOf(ApprovalItem(reference_name = "A")), 2)))
        assertEquals(0, vm.pageIndex)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun staleApprovalPageCannotOverwriteNewPageOrDismissItsLoader() {
        val vm = approvals(); vm.load(0)
        val old = network.last("getApprovals")
        vm.load(1)
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Failure("stale"))
        assertTrue(vm.listLoading().value!!.isLoading)
        network.complete(network.last("getApprovals"), ApiResult.Success(ApprovalsPage(listOf(ApprovalItem(reference_name = "new")), 20)))
        assertEquals("new", vm.items.single().reference_name)
        assertNull(vm.listLoading().value!!.error)
    }

    @Test fun teamQuotaFallbackIsPartOfSameAtomicBatch() {
        val vm = team(); vm.load()
        network.complete(network.last("getTeamAttendance"), ApiResult.Success(TeamAttendanceData(listOf(TeamMember(id = "1")))))
        network.calls.filter { it.name == "getTeamCheckins" }.toList().forEach {
            network.complete(it, ApiResult.Success(TeamCheckinData()))
        }
        network.complete(network.last("postTeamLeaveQuota"), ApiResult.Failure("old server"))
        assertTrue(vm.listLoading().value!!.isLoading)
        assertTrue(vm.members.isEmpty())
        network.complete(network.last("callTeamLeaveQuota"), ApiResult.Success(TeamQuotaData()))
        assertEquals("1", vm.members.single().id)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun teamFilterReplacementCancelsAllSourcesAndKeepsSnapshotOnFailure() {
        val vm = team(); vm.load()
        val old = network.calls.toList()
        vm.applyFilters(TeamActivityFilters(startDate = "2026-10-01", endDate = "2026-10-31"))
        assertTrue(old.all { it.cancelled })
        old.forEach { network.complete(it, ApiResult.Failure("stale")) }
        assertTrue(vm.listLoading().value!!.isLoading)
        assertFalse(vm.hasLoaded)
    }

    @Test fun teamPartialRefreshNeverMixesNewMembersWithOldQuota() {
        val vm = team(); vm.load()
        fun finishBatch(failQuota: Boolean) {
            network.complete(network.last("getTeamAttendance"), ApiResult.Success(TeamAttendanceData(listOf(TeamMember(id = if (failQuota) "new" else "old")))))
            network.calls.filter { it.name == "getTeamCheckins" }.takeLast(2).forEach {
                network.complete(it, ApiResult.Success(TeamCheckinData()))
            }
            if (failQuota) {
                network.complete(network.last("postTeamLeaveQuota"), ApiResult.Failure("offline"))
                network.complete(network.last("callTeamLeaveQuota"), ApiResult.Failure("offline"))
            } else network.complete(network.last("postTeamLeaveQuota"), ApiResult.Success(TeamQuotaData()))
        }
        finishBatch(false)
        val original = vm.members
        vm.load(ListLoadKind.PULL_REFRESH)
        finishBatch(true)
        assertSame(original, vm.members)
        assertEquals("old", vm.members.single().id)
        assertTrue(vm.listLoading().value!!.error!!.contains("previous=true"))
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun calendarMonthChangeCancelsOldRangeAndRetainsCalendarOnFailure() {
        val vm = calendar(); vm.load()
        val old = network.last("getTeamCalendar")
        vm.shiftMonth(-1)
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Failure("stale"))
        val current = network.last("getTeamCalendar")
        val member = TeamCalendarMember(employee_id = "1", employee_name = "One")
        network.complete(current, ApiResult.Success(TeamCalendarData(employees = listOf(member))))
        vm.shiftMonth(-1)
        network.complete(network.last("getTeamCalendar"), ApiResult.Failure("offline"))
        assertEquals(listOf(member), vm.members)
        assertTrue(vm.showingPreviousRange)
        assertEquals("offline", vm.listLoading().value!!.error)
    }

    @Test fun organogramRefreshFailureKeepsExistingTree() {
        val vm = organogram(); vm.reload()
        val node = OrgNode(name = "1", title = "One")
        network.complete(network.last("getTeamOrganogram"), ApiResult.Success(OrganogramData(listOf(node), 1, true)))
        vm.reload()
        network.complete(network.last("getTeamOrganogram"), ApiResult.Failure("offline"))
        assertEquals(listOf(node), vm.nodes)
        assertTrue(vm.hasTeam)
        assertEquals("offline", vm.listLoading().value!!.error)
    }

    @Test fun allRemainingSourcesAreCancelledWhenModelsClear() {
        val e = expenses(); val l = leaves(); val a = approvals(); val t = team(); val c = calendar(); val o = organogram()
        e.loadClaims("2026-11-01", "2026-11-09", "", ListLoadKind.INITIAL)
        l.load(); a.load(); t.load(); c.load(); o.reload()
        clearModels()
        assertTrue(network.calls.all { it.cancelled })
    }
}
