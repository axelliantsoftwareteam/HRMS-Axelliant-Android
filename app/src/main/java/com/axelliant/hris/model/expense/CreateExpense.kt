package com.axelliant.hris.model.expense

data class CreateExpense(
    var expense_id: String?=null,
    var posting_date: String?=null,
    var expense_details: ArrayList<AddExpense>?=null,
    var total_amount: String?=null
)