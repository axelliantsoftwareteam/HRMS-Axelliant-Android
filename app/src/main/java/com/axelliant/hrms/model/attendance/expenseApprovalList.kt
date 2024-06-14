package com.axelliant.hrms.model.attendance
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.model.expense.Expense

data class expenseApprovalList(
        val expenses: ArrayList<Expense>?=null,
        val meta: Meta
)