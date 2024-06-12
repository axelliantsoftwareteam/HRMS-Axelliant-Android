package com.axelliant.hrms.model.expense

data class AddExpense(
    var amount: Int?=null,
    var description: String?=null,
    var expense_date: String?=null,
    var expense_type: String?=null
)