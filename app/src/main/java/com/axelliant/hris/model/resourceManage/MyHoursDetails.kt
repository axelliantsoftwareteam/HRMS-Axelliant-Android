package com.axelliant.hris.model.resourceManage

import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.expense.Expense

data class MyHoursDetails(
//    val expense_status:  ArrayList<FilterModel>? = null,
    val resource_hour_data:  ArrayList<DocumentHours>? = null,
    val meta: Meta
)