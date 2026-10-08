package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.requests.CheckinRequestItem
import com.axelliant.hris.model.requests.CheckinRequestsPayload
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.model.requests.ExpenseClaimsPayload
import com.axelliant.hris.model.requests.LeaveRequestItem
import com.axelliant.hris.model.requests.LeaveRequestsPayload
import com.axelliant.hris.model.requests.RequestRow
import com.axelliant.hris.model.requests.toAmount
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * "Your latest activity": the employee's check-in, leave and expense requests for a period, merged
 * into one list, newest first. The three calls run in parallel and the list is replaced only when
 * all of them have answered.
 */
class RequestsOverviewViewModel(private val api: ApiInterface) : BaseViewModel() {

    enum class Period { LAST_7_DAYS, THIS_MONTH }

    /** The period shown; the screen restores its toggle from this. */
    var period: Period = Period.LAST_7_DAYS

    private val _rows = MutableLiveData<List<RequestRow>?>()

    /** The loaded rows; null until the first load has finished. */
    val rows: LiveData<List<RequestRow>?> = _rows

    private val _failed = MutableLiveData<Event<Boolean>>()

    /** Fires once per load in which at least one of the three calls failed. */
    val failed: LiveData<Event<Boolean>> = _failed

    private var generation = 0

    /** [showLoading] is false for pull-to-refresh and for reloads of a list that already has rows. */
    fun load(showLoading: Boolean) {
        val current = ++generation
        val (start, end) = range(period)
        if (showLoading) isLoading.value = Event(true)

        var pending = 3
        var anyFailed = false
        var leaves: List<RequestRow> = emptyList()
        var checkins: List<RequestRow> = emptyList()
        var expenses: List<RequestRow> = emptyList()

        fun finishOne() {
            pending--
            if (pending > 0) return
            if (showLoading) isLoading.value = Event(false)
            if (current != generation) return
            _rows.value = (leaves + checkins + expenses).sortedByDescending { sortKey(it) }
            if (anyFailed) _failed.value = Event(true)
        }

        val employeeId = GlobalConfig.currentEmployeeId()
        api.postLeaveRequestsMap(
            AppConst.authHeader(),
            mapOf(
                "start_date" to start,
                "end_date" to end,
                "employee_list" to if (employeeId.isBlank()) emptyList() else listOf(employeeId),
                "filters" to ""
            )
        ).enqueueResult<LeaveRequestsPayload>(object : TypeToken<LeaveRequestsPayload>() {}.type) { result ->
            when (result) {
                is ApiResult.Success -> leaves = result.data.leaves.orEmpty().map(::leaveRow)
                is ApiResult.Failure -> anyFailed = true
            }
            finishOne()
        }

        api.postCheckinsMap(
            AppConst.authHeader(),
            mapOf("start_date" to start, "end_date" to end, "filters" to listOf(""))
        ).enqueueResult<CheckinRequestsPayload>(object : TypeToken<CheckinRequestsPayload>() {}.type) { result ->
            when (result) {
                is ApiResult.Success -> checkins = result.data.checkin.orEmpty().map(::checkinRow)
                is ApiResult.Failure -> anyFailed = true
            }
            finishOne()
        }

        api.callMyExpenseDetail(
            AppConst.authHeader(),
            AttRequest().apply {
                start_date = start
                end_date = end
            }
        ).enqueueResult<ExpenseClaimsPayload>(object : TypeToken<ExpenseClaimsPayload>() {}.type) { result ->
            when (result) {
                is ApiResult.Success -> expenses = result.data.expenses.orEmpty().map(::expenseRow)
                is ApiResult.Failure -> anyFailed = true
            }
            finishOne()
        }
    }

    // ---- mapping ----

    private fun leaveRow(item: LeaveRequestItem) = RequestRow(
        kind = RequestRow.Kind.LEAVE,
        reference = item.name.orEmpty(),
        rawTitle = item.leave_type.orEmpty(),
        reason = item.leave_reason.orEmpty(),
        status = WsStatus.displayLeave(item.status),
        location = "",
        effectiveOn = item.from_date.orEmpty(),
        fromDate = item.from_date.orEmpty(),
        toDate = item.to_date.orEmpty(),
        workingHours = 0.0,
        totalDays = item.total_leave_days.toAmount(),
        totalAmount = 0.0,
        submittedOn = item.post_date.orEmpty()
    )

    private fun checkinRow(item: CheckinRequestItem) = RequestRow(
        kind = RequestRow.Kind.CHECKIN,
        reference = item.name.orEmpty(),
        rawTitle = if (item.log_type.orEmpty().uppercase() == "OUT") "CHECK OUT" else "CHECK IN",
        reason = item.reason.orEmpty(),
        status = item.requeststatus.orEmpty(),
        location = item.location.orEmpty(),
        effectiveOn = item.time.orEmpty(),
        fromDate = "",
        toDate = "",
        workingHours = item.working_hours.toAmount(),
        totalDays = 0.0,
        totalAmount = 0.0,
        submittedOn = item.creation.orEmpty()
    )

    private fun expenseRow(item: ExpenseClaim) = RequestRow(
        kind = RequestRow.Kind.EXPENSE,
        reference = item.name.orEmpty(),
        rawTitle = "",
        reason = "",
        status = item.displayStatus(),
        location = "",
        effectiveOn = item.posting_date.orEmpty(),
        fromDate = "",
        toDate = "",
        workingHours = 0.0,
        totalDays = 0.0,
        totalAmount = item.amount,
        submittedOn = ""
    )

    /** Orders rows by when they happened; rows with an unreadable date go last. */
    private fun sortKey(row: RequestRow): Long = WsFormat.parse(row.effectiveOn)?.time ?: Long.MIN_VALUE

    private fun range(period: Period): Pair<String, String> {
        val today = Calendar.getInstance()
        return when (period) {
            Period.LAST_7_DAYS -> {
                val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
                WsFormat.apiDay(start) to WsFormat.apiDay(today)
            }
            Period.THIS_MONTH -> {
                val first = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                val last = (today.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                WsFormat.apiDay(first) to WsFormat.apiDay(last)
            }
        }
    }
}
