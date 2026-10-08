package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.leaveatt.AttendancePayload
import com.axelliant.hris.model.leaveatt.WeekStatsPayload
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult

/** Attendance screen: the shift card and the day-by-day attendance of one employee. */
class AttendanceViewModel(private val api: ApiInterface) : BaseViewModel() {

    val shift: MutableLiveData<Event<ApiResult<WeekStatsPayload>>> by lazy { MutableLiveData() }
    val attendance: MutableLiveData<Event<ApiResult<AttendancePayload>>> by lazy { MutableLiveData() }

    /** The shift is a nice-to-have, so it never shows the loading overlay. */
    fun loadShift() {
        request(api.callAttendanceWeekStats(AppConst.authHeader()), WeekStatsPayload::class.java, shift, false)
    }

    /**
     * [startDate] / [endDate] are `yyyy-MM-dd`; [filterId] is the status chip id ("" for all).
     * [showLoading] is false while pull-to-refresh already shows its own spinner.
     */
    fun loadAttendance(startDate: String, endDate: String, employeeId: String, filterId: String, showLoading: Boolean = true) {
        val body = AttRequest().apply {
            start_date = startDate
            end_date = endDate
            filters = filterId
            employee_list = listOfNotNull(employeeId.takeIf { it.isNotBlank() })
        }
        request(api.callAttendanceDetail(AppConst.authHeader(), body), AttendancePayload::class.java, attendance, showLoading)
    }
}
