package com.axelliant.hrms.model.expense

import com.axelliant.hrms.model.base.Meta

data class MyExpensePostResponse(
    val expense_detail: ExpenseDetail?=null,
    val status_message:String?=null,
    val meta: Meta
)