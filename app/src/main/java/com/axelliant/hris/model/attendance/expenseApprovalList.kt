package com.axelliant.hris.model.attendance
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.expense.Expense

data class expenseApprovalList(
        val expenses: ArrayList<Expense>?=null,
        val meta: Meta
)