package com.axelliant.hrms.model.expense

import com.axelliant.hrms.model.leave.SpinnerType

data class AddExpense(
    var amount: Double?=null,
    var description: String?=null,
    var expense_date: String?=null,
    var expense_type: String?=null,
    var expenseTypeList: ArrayList<SpinnerType> = arrayListOf()

)