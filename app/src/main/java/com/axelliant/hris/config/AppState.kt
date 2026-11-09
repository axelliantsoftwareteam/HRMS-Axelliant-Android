package com.axelliant.hris.config

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.model.dashboard.EmployProfile

/** App-wide observable state shared by Home, the navigation drawer and the notification badge. */
object AppState {
    /** Signed-in employee from the dashboard (drives the drawer header and manager section). */
    val currentEmployee = MutableLiveData<EmployProfile?>()

    /** Unread notification count for the bell / drawer pill. */
    val unreadNotifications = MutableLiveData(0)

    fun reset() {
        com.axelliant.hris.cache.HomeMemoryCache.clear()
        GlobalConfig.setCurrentEmployee(null)
        unreadNotifications.value = 0
    }
}
