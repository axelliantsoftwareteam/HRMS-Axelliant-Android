package com.axelliant.hris.model.expense

/**
 * One expense line. This is both the request line of `create_expenses`/`update_expense` and the
 * shape of the Gson list passed to the edit screen in `AppConst.ExpenseRequestParam`, so it only
 * holds fields the server expects (client-only state lives in `ExpenseLineDraft`).
 */
data class AddExpense(
    var amount: Double? = 0.0,
    var description: String = "",
    var expense_date: String? = null,
    var expense_type: String? = null
)
