package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.leaveatt.LeaveHistoryPayload
import com.axelliant.hris.model.leaveatt.LeaveRecord
import com.axelliant.hris.model.leaveatt.LeaveQuotaPayload
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import java.util.Calendar

/** Leave screen: assigned quotas (`get_leaves_detail`) and this year's leave requests. */
class LeaveViewModel(private val api: ApiInterface) : BaseViewModel() {

    val quotas: MutableLiveData<Event<ApiResult<LeaveQuotaPayload>>> by lazy { MutableLiveData() }
    val history: MutableLiveData<Event<ApiResult<LeaveHistoryPayload>>> by lazy { MutableLiveData() }

    /** The request whose details sheet is open; kept here so "Edit request" survives a rotation. */
    var selectedLeave: LeaveRecord? = null

    /** [showLoading] is true only for the very first balances load; later loads refresh silently. */
    fun loadQuotas(showLoading: Boolean) {
        request(api.getLeaveTypesWithCount(AppConst.authHeader()), LeaveQuotaPayload::class.java, quotas, showLoading)
    }

    /** All of the signed-in employee's leave requests for the current calendar year. */
    fun loadHistory() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val body = AttRequest().apply {
            start_date = "$year-01-01"
            end_date = "$year-12-31"
            filters = ""
            employee_list = listOfNotNull(GlobalConfig.currentEmployeeId().takeIf { it.isNotBlank() })
        }
        request(api.callMyLeaveDetail(AppConst.authHeader(), body), LeaveHistoryPayload::class.java, history, false)
    }
}
