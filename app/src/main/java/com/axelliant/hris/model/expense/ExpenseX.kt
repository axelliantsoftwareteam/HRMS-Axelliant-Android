package com.axelliant.hris.model.expense

data class ExpenseX(
    val __unsaved: Int,
    val amount: Double,
    val creation: String,
    val default_account: String,
    val description: String,
    val docstatus: Int,
    val doctype: String,
    val expense_date: String,
    val expense_type: String,
    val idx: Int,
    val modified: String,
    val modified_by: String,
    val name: String,
    val owner: String,
    val parent: String,
    val parentfield: String,
    val parenttype: String,
    val sanctioned_amount: Double
)