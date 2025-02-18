package com.axelliant.hris.model.expense

import com.axelliant.hris.model.leave.SpinnerType

data class AddExpense(
    var amount: Double?=0.0,
    var description: String="",
    var expense_date: String?=null,
    var expense_type: String?=null,
    var expenseTypeList: ArrayList<SpinnerType> = arrayListOf()

)

