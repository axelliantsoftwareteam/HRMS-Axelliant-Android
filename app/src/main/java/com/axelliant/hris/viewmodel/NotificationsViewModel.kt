package com.axelliant.hris.viewmodel

import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppState
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

class NotificationsViewModel(private val api: ApiInterface) : BaseViewModel() {

    private data class UnreadPayload(val unread_count: Any? = null)

    /** Silent refresh of the unread badge shown on Home and in the drawer. Failures are ignored. */
    fun refreshUnreadCount() {
        api.getNotifications(AppConst.authHeader(), 1, 0)
            .enqueueResult<UnreadPayload>(UnreadPayload::class.java) { result ->
                if (result is ApiResult.Success) {
                    val count = result.data.unread_count?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    AppState.unreadNotifications.value = count
                }
            }
    }
}
