package com.axelliant.hris.model.expense

import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.FilterModel

data class MyExpenseDetailResponse(
    val expense_status:  ArrayList<FilterModel>? = null,
    val expense_total: Int? = null,
    val expenses:  ArrayList<Expense>? = null,
    val meta: Meta
)