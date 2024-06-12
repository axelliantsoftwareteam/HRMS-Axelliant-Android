package com.axelliant.hrms.model.expense

import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.FilterModel

data class MyExpenseDetailResponse(
    val expense_status:  ArrayList<FilterModel>? = null,
    val expense_total: Int? = null,
    val expenses:  ArrayList<Expense>? = null,
    val meta: Meta
)