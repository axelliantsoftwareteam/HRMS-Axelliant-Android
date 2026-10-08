package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.leaveatt.ActionPayload
import com.axelliant.hris.model.leaveatt.CheckinOptionsPayload
import com.axelliant.hris.model.leaveatt.LeaveDaysPayload
import com.axelliant.hris.model.leaveatt.LeaveQuotaPayload
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult

/** Leave and attendance request forms: lookups plus create / update / delete. */
class RequestViewModel(private val api: ApiInterface) : BaseViewModel() {

    val quotas: MutableLiveData<Event<ApiResult<LeaveQuotaPayload>>> by lazy { MutableLiveData() }
    val options: MutableLiveData<Event<ApiResult<CheckinOptionsPayload>>> by lazy { MutableLiveData() }
    val leaveDays: MutableLiveData<Event<ApiResult<LeaveDaysPayload>>> by lazy { MutableLiveData() }

    /** Result of the last create / update / delete call. */
    val action: MutableLiveData<Event<ApiResult<ActionPayload>>> by lazy { MutableLiveData() }

    private fun auth() = AppConst.authHeader()

    fun loadLeaveQuotas() =
        request(api.getLeaveTypesWithCount(auth()), LeaveQuotaPayload::class.java, quotas)

    fun loadOptions() =
        request(api.getAttendanceRequestInformation(auth()), CheckinOptionsPayload::class.java, options)

    /** Server-side day count; silent because it re-runs on every type or date change. */
    fun loadLeaveDays(leaveType: String, from: String, to: String, halfDay: Boolean, halfDayDate: String?) {
        val body = mutableMapOf<String, Any?>("leave_type" to leaveType, "from_date" to from, "to_date" to to)
        if (halfDay) {
            body["half_day"] = true
            if (!halfDayDate.isNullOrBlank()) body["half_day_date"] = halfDayDate
        }
        request(api.fetchLeaveDays(auth(), body), LeaveDaysPayload::class.java, leaveDays, false)
    }

    fun createLeave(body: Map<String, Any?>) = send(api.submitLeaveApplication(auth(), body))

    fun updateLeave(body: Map<String, Any?>) = send(api.changeLeaveApplication(auth(), body))

    fun deleteLeave(leaveId: String) = send(api.cancelLeaveApplication(auth(), mapOf("leave_id" to leaveId)))

    fun createAttendance(body: Map<String, Any?>) = send(api.submitCheckinRequest(auth(), body))

    fun updateAttendance(body: Map<String, Any?>) = send(api.changeCheckinRequest(auth(), body))

    fun deleteAttendance(checkinId: String) = send(api.cancelCheckinRequest(auth(), mapOf("checkin_id" to checkinId)))

    private fun send(call: retrofit2.Call<okhttp3.ResponseBody>) =
        request(call, ActionPayload::class.java, action)
}
