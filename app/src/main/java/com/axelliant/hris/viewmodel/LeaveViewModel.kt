package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.leaveatt.LeaveHistoryPayload
import com.axelliant.hris.model.leaveatt.LeaveRecord
import com.axelliant.hris.model.leaveatt.LeaveQuotaPayload
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import java.util.Calendar

/** Quotas/history commit together so a partial refresh never replaces a complete snapshot. */
open class LeaveViewModel(private val api: ApiInterface, private val batchFailure: (Boolean) -> String) : BaseViewModel() {
    data class Snapshot(val quotas: LeaveQuotaPayload, val history: LeaveHistoryPayload)
    private val _data = MutableLiveData<Snapshot?>()
    val data: LiveData<Snapshot?> = _data
    var selectedLeave: LeaveRecord? = null

    fun load(kind: ListLoadKind = ListLoadKind.REFRESH) {
        val ticket = beginListLoad(kind = kind, replace = true)
        var pending = 2
        var quotas: LeaveQuotaPayload? = null
        var history: LeaveHistoryPayload? = null
        fun finish() {
            if (!isCurrentListLoad("list", ticket) || --pending > 0) return
            val q = quotas; val h = history
            if (q != null && h != null) {
                _data.value = Snapshot(q, h)
                finishListLoad("list", ticket, q.allocations.size + h.leaves.orEmpty().size)
            } else finishListLoad("list", ticket, error = batchFailure(_data.value != null))
        }
        val auth = AppConst.authHeader()
        requestListPart<LeaveQuotaPayload>(api.getLeaveTypesWithCount(auth), LeaveQuotaPayload::class.java, "list", ticket) {
            if (it is ApiResult.Success) quotas = it.data
            finish()
        }
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val body = AttRequest().apply {
            start_date = "$year-01-01"; end_date = "$year-12-31"; filters = ""
            employee_list = listOfNotNull(GlobalConfig.currentEmployeeId().takeIf { it.isNotBlank() })
        }
        requestListPart<LeaveHistoryPayload>(api.callMyLeaveDetail(auth, body), LeaveHistoryPayload::class.java, "list", ticket) {
            if (it is ApiResult.Success) history = it.data
            finish()
        }
    }
}
