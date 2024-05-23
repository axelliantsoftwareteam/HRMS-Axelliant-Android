package com.axelliant.android_erp.model.dashboard
import com.axelliant.android_erp.model.base.Meta

data class DashboardResponse(
        val birthday_data: ArrayList<Birthday>?=null,
        val employee_profile:EmployProfile?=null,
        val meta: Meta
)