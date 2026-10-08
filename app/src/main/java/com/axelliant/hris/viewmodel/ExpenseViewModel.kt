package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.requests.ExpenseClaimsPayload
import com.axelliant.hris.model.requests.ExpenseSavePayload
import com.axelliant.hris.model.requests.ExpenseTypesPayload
import com.axelliant.hris.model.requests.StatusPayload
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult
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
class ExpenseViewModel(private val expenseRepo: ExpenseRepo, private val validator: Validator) : BaseViewModel() {

    val claims = MutableLiveData<Event<ApiResult<ExpenseClaimsPayload>>>()
    val approvals = MutableLiveData<Event<ApiResult<ExpenseClaimsPayload>>>()
    val expenseTypes = MutableLiveData<Event<ApiResult<ExpenseTypesPayload>>>()
    val saveResult = MutableLiveData<Event<ApiResult<ExpenseSavePayload>>>()
    val uploadResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val deleteClaimResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val deleteAttachmentResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()
    val decisionResult = MutableLiveData<Event<ApiResult<StatusPayload>>>()

    private var claimsGeneration = 0
    private var approvalsGeneration = 0

    /**
     * Loads the employee's claims. When the period or filter changes quickly only the newest answer is
     * delivered. [showLoading] is false for pull-to-refresh and reloads of a list that already has data.
     */
    fun loadClaims(startDate: String, endDate: String, statusFilter: String, showLoading: Boolean) {
        val generation = ++claimsGeneration
        if (showLoading) isLoading.value = Event(true)
        expenseRepo.claims(startDate, endDate, statusFilter)
            .enqueueResult<ExpenseClaimsPayload>(typeOf<ExpenseClaimsPayload>()) { result ->
                if (showLoading) isLoading.value = Event(false)
                if (generation == claimsGeneration) claims.value = Event(result)
            }
    }

    fun loadApprovals(startDate: String, endDate: String, showLoading: Boolean) {
        val generation = ++approvalsGeneration
        if (showLoading) isLoading.value = Event(true)
        expenseRepo.approvals(startDate, endDate)
            .enqueueResult<ExpenseClaimsPayload>(typeOf<ExpenseClaimsPayload>()) { result ->
                if (showLoading) isLoading.value = Event(false)
                if (generation == approvalsGeneration) approvals.value = Event(result)
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
