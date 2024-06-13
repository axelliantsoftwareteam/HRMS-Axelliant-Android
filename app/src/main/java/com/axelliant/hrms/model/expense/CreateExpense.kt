package com.axelliant.hrms.model.expense

data class CreateExpense(
    var posting_date: String?=null,
    var expense_details: ArrayList<AddExpense>?=null,
    var total_amount: String?=null
)