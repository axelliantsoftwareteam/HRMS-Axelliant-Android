package com.axelliant.hris.viewmodel

import com.axelliant.hris.model.notification.NotificationPage
import com.axelliant.hris.model.notification.WorkflowNotification
import com.axelliant.hris.model.payslip.SalarySlip
import com.axelliant.hris.model.payslip.SalarySlipPage
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.config.AppState
import java.lang.reflect.Type
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Call

class PagedLoadingTest : ListScreenTestSupport() {
    private val network = ListNetworkHarness()
    private fun pay() = retain(object : PayslipsViewModel(network.api) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun notifications() = retain(object : NotificationsViewModel(network.api) {
        override fun <T> enqueue(call: Call<ResponseBody>, type: Type, complete: (ApiResult<T>) -> Unit) = network.register(call, complete)
    })
    private fun payPage(vararg names: String, total: Int = 5) = SalarySlipPage(names.map { SalarySlip(name = it) }, total)
    private fun notificationPage(vararg names: String, total: Int = 5, unread: Int = 2) =
        NotificationPage(names.map { WorkflowNotification(name = it, read = 0) }, unread, total)

    @Test fun payslipsKeepPageSizeOffsetDeduplicationAndExhaustion() {
        val vm = pay()
        vm.reload()
        assertTrue(vm.listLoading().value!!.initialLoading)
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("1", "2")))
        vm.loadMore()
        val page = network.last("getSalarySlips")
        assertEquals(12, page.args[1]); assertEquals(2, page.args[2])
        assertTrue(vm.state.value!!.loadingMore)
        assertTrue(vm.listLoading().value!!.loadingMore)
        network.complete(page, ApiResult.Success(payPage("2", "3")))
        assertEquals(listOf("1", "2", "3"), vm.state.value!!.items.map { it.name })
        vm.loadMore()
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("1", "2")))
        val count = network.calls.size
        vm.loadMore()
        assertEquals(count, network.calls.size)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun payslipPageRetryKeepsRowsAndRetriesSameOffset() {
        val vm = pay(); vm.reload()
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("1", "2")))
        vm.loadMore()
        network.complete(network.last("getSalarySlips"), ApiResult.Failure("offline"))
        assertEquals(2, vm.state.value!!.items.size)
        assertEquals("offline", vm.listLoading().value!!.error)
        val count = network.calls.size
        vm.loadMore(); assertEquals(count, network.calls.size)
        vm.retryLoad()
        assertEquals(2, network.last("getSalarySlips").args[2])
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("3")))
        assertEquals(3, vm.state.value!!.items.size)
    }

    @Test fun payslipRefreshCancelsOldPageAndLatePageCannotOverwriteIt() {
        val vm = pay(); vm.reload()
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("1", "2")))
        vm.loadMore(); val old = network.last("getSalarySlips")
        vm.reload(pull = true)
        val refresh = network.last("getSalarySlips")
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Success(payPage("old")))
        assertTrue(vm.listLoading().value!!.pullRefreshing)
        assertEquals(2, vm.state.value!!.items.size)
        network.complete(refresh, ApiResult.Success(payPage("new", total = 1)))
        assertEquals(listOf("new"), vm.state.value!!.items.map { it.name })
        assertFalse(vm.state.value!!.loadingMore)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun payslipRefreshFailureRetryStartsAtZeroNotPreviousFailedPage() {
        val vm = pay(); vm.reload()
        network.complete(network.last("getSalarySlips"), ApiResult.Success(payPage("1", "2")))
        vm.loadMore(); network.complete(network.last("getSalarySlips"), ApiResult.Failure("page error"))
        vm.reload(pull = true); network.complete(network.last("getSalarySlips"), ApiResult.Failure("refresh error"))
        vm.retryLoad()
        assertEquals(0, network.last("getSalarySlips").args[2])
        assertEquals(2, vm.state.value!!.items.size)
    }

    @Test fun notificationsKeepPageSizeAndFailedPageRetry() {
        val vm = notifications(); vm.reload()
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        vm.loadMore()
        assertEquals(20, network.last("getNotifications").args[1])
        assertEquals(2, network.last("getNotifications").args[2])
        network.complete(network.last("getNotifications"), ApiResult.Failure("offline"))
        vm.retryLoad()
        assertEquals(2, network.last("getNotifications").args[2])
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("2", "3")))
        assertEquals(listOf("1", "2", "3"), vm.state.value!!.items.map { it.name })
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun notificationRefreshReplacesPendingPageWithoutMixingData() {
        val vm = notifications(); vm.reload()
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        vm.loadMore(); val old = network.last("getNotifications")
        vm.reload(pull = true)
        val refresh = network.last("getNotifications")
        assertTrue(old.cancelled)
        network.complete(old, ApiResult.Failure("old error"))
        assertTrue(vm.listLoading().value!!.pullRefreshing)
        assertNull(vm.listLoading().value!!.error)
        network.complete(refresh, ApiResult.Success(notificationPage("new", total = 1)))
        assertEquals(listOf("new"), vm.state.value!!.items.map { it.name })
        assertFalse(vm.state.value!!.canLoadMore)
    }

    @Test fun markAllReadCannotBeUndoneByLatePageAndDoesNotRepeatMutation() {
        val vm = notifications(); vm.reload()
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        vm.loadMore(); val old = network.last("getNotifications")
        vm.markAllRead(); vm.markAllRead()
        assertTrue(old.cancelled)
        assertEquals(1, network.calls.count { it.name == "markAllNotificationsRead" })
        network.complete(old, ApiResult.Success(notificationPage("3")))
        network.complete(network.last("markAllNotificationsRead"), ApiResult.Success(Any()))
        assertEquals(0, vm.state.value!!.unread)
        assertTrue(vm.state.value!!.items.all { it.isRead })
        assertFalse(vm.state.value!!.updating)
        assertFalse(vm.listLoading().value!!.isLoading)
    }

    @Test fun notificationDuplicatePageStopsAutomaticRequests() {
        val vm = notifications(); vm.reload()
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        vm.loadMore(); network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        val count = network.calls.size
        vm.loadMore()
        assertEquals(count, network.calls.size)
    }

    @Test fun lateHomeBadgeFetchCannotUndoMarkAllReadInAnotherScreen() {
        val homeBadge = notifications()
        val feed = notifications()
        homeBadge.refreshUnreadCount()
        val oldBadge = network.last("getNotifications")
        feed.reload()
        network.complete(network.last("getNotifications"), ApiResult.Success(notificationPage("1", "2")))
        feed.markAllRead()
        network.complete(network.last("markAllNotificationsRead"), ApiResult.Success(Any()))
        network.complete(oldBadge, ApiResult.Success(NotificationsViewModel.UnreadPayload(99)))
        assertEquals(0, AppState.unreadNotifications.value)
    }

    @Test fun clearingPagedModelsCancelsPendingReadsAndRejectsLateData() {
        val pay = pay(); val notifications = notifications()
        pay.reload(); notifications.reload()
        val calls = network.calls.toList()
        clearModels()
        assertTrue(calls.all { it.cancelled })
        network.complete(calls[0], ApiResult.Success(payPage("late")))
        network.complete(calls[1], ApiResult.Success(notificationPage("late")))
        assertTrue(pay.state.value!!.items.isEmpty())
        assertTrue(notifications.state.value!!.items.isEmpty())
    }
}
