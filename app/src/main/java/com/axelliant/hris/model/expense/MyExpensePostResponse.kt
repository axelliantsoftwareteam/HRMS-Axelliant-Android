package com.axelliant.hris.model.expense

import com.axelliant.hris.model.base.Meta

data class MyExpensePostResponse(
    val expense_detail: ExpenseDetail?=null,
    val status_message:String?=null,
    val meta: Meta
)