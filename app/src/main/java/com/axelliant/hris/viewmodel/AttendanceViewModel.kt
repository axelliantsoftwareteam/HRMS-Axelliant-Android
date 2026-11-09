package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.leaveatt.AttendancePayload
import com.axelliant.hris.model.leaveatt.ShiftDetail
import com.axelliant.hris.model.leaveatt.WeekStatsPayload
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.ui.WsFormat
import okhttp3.ResponseBody
import retrofit2.Call
import java.util.Calendar

/** Retains selection/content; attendance and optional shift have separate cancellable lifetimes. */
open class AttendanceViewModel(private val api: ApiInterface, private val savedState: SavedStateHandle) : BaseViewModel() {
    enum class Period { WEEK, MONTH, CUSTOM }
    data class Selection(val employeeId: String, val period: Period, val from: String, val to: String, val filter: String)

    private val _selection = MutableLiveData<Selection>()
    val selection: LiveData<Selection> = _selection
    private val _attendance = MutableLiveData<AttendancePayload?>()
    val attendance: LiveData<AttendancePayload?> = _attendance
    private val _shift = MutableLiveData<ShiftDetail?>()
    val shift: LiveData<ShiftDetail?> = _shift
    var displayedSelection: Selection? = null
        private set
    private var shiftCall: Call<ResponseBody>? = null
    private var shiftLoading = false

    fun initialize(employeeId: String) {
        if (_selection.value?.employeeId == employeeId) return
        clearListLoading()
        _attendance.value = null
        displayedSelection = null
        val restoring = savedState.get<String>("att-employee") == employeeId
        val period = if (restoring) Period.values().firstOrNull { it.name == savedState.get<String>("att-period") } ?: Period.WEEK else Period.WEEK
        val range = if (restoring) savedState.get<String>("att-from")?.let { from ->
            savedState.get<String>("att-to")?.let { to -> from to to }
        } else null
        val dates = range ?: preset(if (period == Period.CUSTOM) Period.WEEK else period)
        save(Selection(employeeId, if (range == null && period == Period.CUSTOM) Period.WEEK else period,
            dates.first, dates.second, if (restoring) savedState.get<String>("att-filter").orEmpty() else ""))
    }

    fun ensureLoaded() {
        val state = listLoading().value
        if (_attendance.value == null && state?.isLoading != true && state?.error == null) reload(ListLoadKind.INITIAL)
        loadShift()
    }

    fun selectPeriod(period: Period) {
        val current = _selection.value ?: return
        if (period == Period.CUSTOM || current.period == period) return
        val range = preset(period)
        save(current.copy(period = period, from = range.first, to = range.second))
        reload(ListLoadKind.FILTER)
    }

    fun setCustomRange(start: Long, end: Long) {
        val current = _selection.value ?: return
        val range = RequestsDateRange.selected(start, end)
        save(current.copy(period = Period.CUSTOM, from = range.first, to = range.second))
        reload(ListLoadKind.FILTER)
    }

    fun selectFilter(id: String) {
        val current = _selection.value ?: return
        if (id == current.filter) return
        save(current.copy(filter = id))
        reload(ListLoadKind.FILTER)
    }

    fun reload(kind: ListLoadKind = ListLoadKind.REFRESH) {
        val selected = _selection.value ?: return
        val ticket = beginListLoad(kind = kind, replace = true)
        val body = AttRequest().apply {
            start_date = selected.from; end_date = selected.to; filters = selected.filter
            employee_list = listOfNotNull(selected.employeeId.takeIf { it.isNotBlank() })
        }
        requestListPart<AttendancePayload>(api.callAttendanceDetail(AppConst.authHeader(), body),
            AttendancePayload::class.java, "list", ticket) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val payload = try {
                        result.data.copy(attendance_data = result.data.attendance_data.orEmpty()
                            .sortedByDescending { WsFormat.parse(it.date)?.time ?: 0L })
                    } catch (_: Exception) {
                        finishListLoad("list", ticket, error = ErrorMessages.UnknownError.errorString)
                        return@requestListPart
                    }
                    displayedSelection = selected
                    _attendance.value = payload
                    finishListLoad("list", ticket, payload.attendance_data.orEmpty().size)
                }
                is ApiResult.Failure -> finishListLoad("list", ticket, error = result.message)
            }
        }
    }

    /** Shift errors are optional and never clear attendance or its loading indication. */
    fun loadShift(force: Boolean = false) {
        if (!force && (shiftLoading || _shift.value != null)) return
        shiftCall?.let(::cancelRequest)
        shiftLoading = true
        val call = api.callAttendanceWeekStats(AppConst.authHeader())
        shiftCall = call
        requestData<WeekStatsPayload>(call, WeekStatsPayload::class.java) { result ->
            shiftLoading = false
            shiftCall = null
            if (result is ApiResult.Success) _shift.value = result.data.shift_detail
        }
    }

    private fun save(value: Selection) {
        savedState["att-employee"] = value.employeeId
        savedState["att-period"] = value.period.name
        savedState["att-from"] = value.from
        savedState["att-to"] = value.to
        savedState["att-filter"] = value.filter
        _selection.value = value
    }

    private fun preset(period: Period): Pair<String, String> {
        val cal = Calendar.getInstance()
        return if (period == Period.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val start = WsFormat.apiDay(cal)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            start to WsFormat.apiDay(cal)
        } else {
            val end = WsFormat.apiDay(cal)
            // Preserve the existing Attendance range semantics.
            cal.add(Calendar.DAY_OF_YEAR, -7)
            WsFormat.apiDay(cal) to end
        }
    }
}
