package com.axelliant.hris.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.requests.*
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import java.util.Calendar

/** Three cancellable API reads form one atomic snapshot. Failed batches retain the last complete result. */
open class RequestsOverviewViewModel(
    private val api: ApiInterface,
    private val savedState: SavedStateHandle,
    private val incompleteMessage: (List<Source>, Boolean) -> String
) : BaseViewModel() {
    enum class Period { LAST_7_DAYS, THIS_MONTH, CUSTOM }
    enum class Source(@StringRes val label: Int) {
        LEAVE(R.string.rq_source_leave), ATTENDANCE(R.string.rq_source_attendance), EXPENSE(R.string.rq_source_expense)
    }

    var period: Period
        get() = Period.values().firstOrNull { it.name == savedState.get<String>("requests-period") } ?: Period.LAST_7_DAYS
        set(value) { savedState["requests-period"] = value.name }

    fun setCustomRange(start: Long, end: Long) {
        val range = RequestsDateRange.selected(start, end)
        savedState["requests-range"] = arrayListOf(range.first, range.second)
        period = Period.CUSTOM
    }

    fun currentRange(): Pair<String, String> {
        if (period == Period.CUSTOM) {
            savedState.get<ArrayList<String>>("requests-range")?.takeIf { it.size == 2 }
                ?.let { return it[0] to it[1] }
        }
        return RequestsDateRange.preset(if (period == Period.CUSTOM) Period.LAST_7_DAYS else period, Calendar.getInstance())
    }

    private val _rows = MutableLiveData<List<RequestRow>?>()
    val rows: LiveData<List<RequestRow>?> = _rows
    var displayedRange: Pair<String, String>? = null
        private set
    private var activeRange: Pair<String, String>? = null

    fun load(kind: ListLoadKind = ListLoadKind.REFRESH, force: Boolean = false) {
        val range = currentRange()
        if (!force && listLoading().value?.isLoading == true && activeRange == range) return
        activeRange = range
        val ticket = beginListLoad(kind = kind, replace = true)
        val (start, end) = range
        val auth = AppConst.authHeader()
        var pending = 3
        val failures = mutableListOf<Source>()
        var leaves: List<RequestRow> = emptyList()
        var checkins: List<RequestRow> = emptyList()
        var expenses: List<RequestRow> = emptyList()

        fun finishOne() {
            if (!isCurrentListLoad("list", ticket)) return
            pending--
            if (pending > 0) return
            if (failures.isEmpty()) {
                val merged = (leaves + checkins + expenses).sortedByDescending { sortKey(it) }
                displayedRange = range
                _rows.value = merged
                finishListLoad("list", ticket, merged.size)
            } else {
                // No category is silently removed and no old/new date ranges are mixed.
                finishListLoad("list", ticket, itemCount = _rows.value?.size,
                    error = incompleteMessage(failures.sortedBy { it.ordinal }, _rows.value != null))
            }
        }

        val employeeId = GlobalConfig.currentEmployeeId()
        requestListPart<LeaveRequestsPayload>(api.postLeaveRequestsMap(auth, mapOf(
            "start_date" to start, "end_date" to end,
            "employee_list" to if (employeeId.isBlank()) emptyList() else listOf(employeeId), "filters" to ""
        )), LeaveRequestsPayload::class.java, "list", ticket) { result ->
            try {
                when (result) {
                    is ApiResult.Success -> leaves = result.data.leaves.orEmpty().map(::leaveRow)
                    is ApiResult.Failure -> failures += Source.LEAVE
                }
            } catch (_: Exception) { failures += Source.LEAVE }
            finishOne()
        }
        requestListPart<CheckinRequestsPayload>(api.postCheckinsMap(auth, mapOf(
            "start_date" to start, "end_date" to end, "filters" to listOf("")
        )), CheckinRequestsPayload::class.java, "list", ticket) { result ->
            try {
                when (result) {
                    is ApiResult.Success -> checkins = result.data.checkin.orEmpty().map(::checkinRow)
                    is ApiResult.Failure -> failures += Source.ATTENDANCE
                }
            } catch (_: Exception) { failures += Source.ATTENDANCE }
            finishOne()
        }
        requestListPart<ExpenseClaimsPayload>(api.callMyExpenseDetail(auth, AttRequest().apply {
            start_date = start; end_date = end
        }), ExpenseClaimsPayload::class.java, "list", ticket) { result ->
            try {
                when (result) {
                    is ApiResult.Success -> expenses = result.data.expenses.orEmpty().map(::expenseRow)
                    is ApiResult.Failure -> failures += Source.EXPENSE
                }
            } catch (_: Exception) { failures += Source.EXPENSE }
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

    /** Accepts minute-precision space-separated API timestamps for sorting without changing display/model values. */
    private fun sortKey(row: RequestRow): Long = WsFormat.parse(row.effectiveOn)?.time
        ?: WsFormat.parse(row.effectiveOn.replaceFirst(' ', 'T'))?.time ?: Long.MIN_VALUE

}
