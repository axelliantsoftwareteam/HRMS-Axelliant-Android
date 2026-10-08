package com.axelliant.hris.model.requests

import com.axelliant.hris.model.expense.AddExpense
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.leave.SpinnerType
import java.util.Locale

/** One line of an expense claim as HRIS returns it. */
data class ExpenseLine(
    val expense_type: String? = null,
    val amount: Any? = null,
    val description: String? = null,
    val expense_date: String? = null
)

data class ExpenseAttachment(
    val name: String? = null,
    val file_url: String? = null
)

/** An expense claim (`get_expense_claim_requests`, `get_expense_approvals`). */
data class ExpenseClaim(
    val name: String? = null,
    val employee_name: String? = null,
    val posting_date: String? = null,
    val status: String? = null,
    val approval_status: String? = null,
    val total_claimed_amount: Any? = null,
    val expenses_detail: List<ExpenseLine>? = null,
    val attachments: List<ExpenseAttachment>? = null
) {
    val amount: Double get() = total_claimed_amount.toAmount()

    /** "Pending" for a draft claim, otherwise the approval status or the status ("Unknown" when HRIS sends none). */
    fun displayStatus(unknown: String = "Unknown"): String {
        val raw = approval_status?.takeIf { it.isNotBlank() } ?: status?.takeIf { it.isNotBlank() } ?: unknown
        return if (raw == "Draft") "Pending" else raw
    }

    /** Only a draft claim can be edited or deleted. */
    val isDraft: Boolean get() = status == "Draft"

    /** The claimed amount without decimals ("%.0f"), as the cards show it. */
    fun amountText(): String = String.format(Locale.US, "%.0f", amount)

    /** The lines in the shape the edit screen receives them. */
    fun editLines(): List<AddExpense> = expenses_detail.orEmpty().map {
        AddExpense(
            amount = it.amount.toAmount(),
            description = it.description.orEmpty(),
            expense_date = it.expense_date,
            expense_type = it.expense_type
        )
    }

    fun attachmentList(): List<Attachments> = attachments.orEmpty().map {
        Attachments(file_url = it.file_url, name = it.name)
    }
}

/** A status chip of the expense list: `expense_status[{id,title,count}]`. */
data class ExpenseStatusChip(
    val id: String? = null,
    val title: String? = null,
    val count: String? = null
)

data class ExpenseClaimsPayload(
    val expenses: List<ExpenseClaim>? = null,
    val expense_status: List<ExpenseStatusChip>? = null
)

/** `get_expense_type`. */
data class ExpenseTypesPayload(val expenses: List<SpinnerType>? = null)

data class ExpenseSavedClaim(val name: String? = null)

/** `create_expenses` / `update_expense`. */
data class ExpenseSavePayload(
    val expense_detail: ExpenseSavedClaim? = null,
    val status_message: String? = null
)

/** Any call whose only useful answer is a status message (delete, approve, upload, ...). */
data class StatusPayload(val status_message: String? = null)

/** One expense line being edited on the create/update screen. */
class ExpenseLineDraft(
    val uid: Long,
    var type: String? = null,
    /** "yyyy-MM-dd" */
    var dateApi: String? = null,
    var amountText: String = "",
    var description: String = "",
    var showErrors: Boolean = false
) {
    val amount: Double get() = amountText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

    val isValid: Boolean get() = !type.isNullOrBlank() && !dateApi.isNullOrBlank() && amount > 0.0
}
