package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.requests.ExpenseClaimsPayload
import com.axelliant.hris.model.requests.ExpenseSavePayload
import com.axelliant.hris.model.requests.ExpenseTypesPayload
import com.axelliant.hris.model.requests.StatusPayload
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.repos.ExpenseRepo
import com.axelliant.hris.utils.Validator
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type

/**
 * State for the expense screens: the claim list, create/edit (types, save, attachments, delete) and
 * the manager's approvals. Each screen owns its own instance (`by viewModel()`), so only the streams
 * it observes are used.
 */
open class ExpenseViewModel(private val expenseRepo: ExpenseRepo, private val validator: Validator, private val savedState: SavedStateHandle) : BaseViewModel() {

    val claims = MutableLiveData<ExpenseClaimsPayload?>()
    val approvals = MutableLiveData<ExpenseClaimsPayload?>()
    var displayedClaimsQuery: Triple<String, String, String>? = null
        private set
    val expenseTypes = MutableLiveData<Event<ApiResult<ExpenseTypesPayload>>>()
    val saveResult = MutableLiveData<Event<ApiResult<ExpenseSavePayload>>>()
    val uploadResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val deleteClaimResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val deleteAttachmentResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val decisionResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()

    var selectedPeriod: String
        get() = savedState.get<String>("expense-period") ?: "LAST_7_DAYS"
        set(value) { savedState["expense-period"] = value }
    var selectedStatus: String
        get() = savedState.get<String>("expense-status").orEmpty()
        set(value) { savedState["expense-status"] = value }

    fun loadClaims(startDate: String, endDate: String, statusFilter: String, kind: ListLoadKind) {
        val ticket = beginListLoad("claims", kind, replace = true)
        requestListPart<ExpenseClaimsPayload>(expenseRepo.claims(startDate, endDate, statusFilter),
            typeOf<ExpenseClaimsPayload>(), "claims", ticket) { result ->
            when (result) {
                is ApiResult.Success -> {
                    displayedClaimsQuery = Triple(startDate, endDate, statusFilter)
                    claims.value = result.data
                    finishListLoad("claims", ticket, result.data.expenses.orEmpty().size)
                }
                is ApiResult.Failure -> finishListLoad("claims", ticket, error = result.message)
            }
        }
    }

    fun loadApprovals(startDate: String, endDate: String, kind: ListLoadKind) {
        val ticket = beginListLoad("approvals", kind, replace = true)
        requestListPart<ExpenseClaimsPayload>(expenseRepo.approvals(startDate, endDate),
            typeOf<ExpenseClaimsPayload>(), "approvals", ticket) { result ->
            when (result) {
                is ApiResult.Success -> {
                    approvals.value = result.data
                    finishListLoad("approvals", ticket, result.data.expenses.orEmpty().size)
                }
                is ApiResult.Failure -> finishListLoad("approvals", ticket, error = result.message)
            }
        }
    }
    fun loadExpenseTypes() =
        request(expenseRepo.expenseTypes(), typeOf<ExpenseTypesPayload>(), expenseTypes)

    fun saveClaim(isUpdate: Boolean, body: CreateExpense) =
        request(expenseRepo.saveClaim(isUpdate, body), typeOf<ExpenseSavePayload>(), saveResult)

    fun uploadAttachment(file: File, docName: String) =
        request(expenseRepo.uploadAttachment(file, docName), typeOf<StatusPayload>(), uploadResult)

    fun deleteClaim(expenseId: String) =
        request(expenseRepo.deleteClaim(expenseId), typeOf<StatusPayload>(), deleteClaimResult)

    fun deleteAttachment(fileId: String) =
        request(expenseRepo.deleteAttachment(fileId), typeOf<StatusPayload>(), deleteAttachmentResult)

    /** Approves ([approve] = true) or rejects one claim awaiting the signed-in manager. */
    fun decide(expenseId: String, approve: Boolean) =
        request(
            expenseRepo.decide(expenseId, if (approve) "Approved" else "Rejected"),
            typeOf<StatusPayload>(),
            decisionResult
        )

    private inline fun <reified T> typeOf(): Type = object : TypeToken<T>() {}.type
}
