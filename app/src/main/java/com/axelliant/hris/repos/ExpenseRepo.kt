package com.axelliant.hris.repos

import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.approval.ApprovalActionRequest
import com.axelliant.hris.model.attendance.AttRequest
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.DeleteAttachment
import com.axelliant.hris.network.ApiInterface
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.File
import java.net.URLConnection

/**
 * Builds the expense-related HRIS calls. The view model runs them through `BaseViewModel.request`,
 * which parses the HRIS envelope, so this class only prepares the requests.
 */
class ExpenseRepo(private val apiInterface: ApiInterface) {

    private fun auth() = AppConst.authHeader()

    /** The signed-in employee's claims between [startDate] and [endDate]; [statusFilter] is a status id or "". */
    fun claims(startDate: String, endDate: String, statusFilter: String): Call<ResponseBody> =
        apiInterface.callMyExpenseDetail(auth(), AttRequest().apply {
            start_date = startDate
            end_date = endDate
            filters = statusFilter
        })

    /** The claims waiting for the signed-in manager. */
    fun approvals(startDate: String, endDate: String): Call<ResponseBody> =
        apiInterface.callExpenseApproval(auth(), AttRequest().apply {
            start_date = startDate
            end_date = endDate
            for_approvals = 1
        })

    fun expenseTypes(): Call<ResponseBody> = apiInterface.getExpenseType(auth())

    fun saveClaim(isUpdate: Boolean, body: CreateExpense): Call<ResponseBody> =
        if (isUpdate) apiInterface.callUpdateExp(auth(), body) else apiInterface.callCreateExp(auth(), body)

    fun deleteClaim(expenseId: String): Call<ResponseBody> =
        apiInterface.deleteExpenseCall(auth(), CreateExpense(expense_id = expenseId))

    fun deleteAttachment(fileId: String): Call<ResponseBody> =
        apiInterface.deleteExpenseAttachmentCall(auth(), DeleteAttachment(file_id = fileId))

    /** Approves or rejects one claim through the shared approval endpoint (`status` is "Approved" or "Rejected"). */
    fun decide(expenseId: String, status: String): Call<ResponseBody> =
        apiInterface.takeApprovalAction(
            auth(),
            ApprovalActionRequest(approval_type = "expense", reference_name = expenseId, status = status)
        )

    /** Uploads [file] as a public attachment of the Expense Claim named [docName]. */
    fun uploadAttachment(file: File, docName: String): Call<ResponseBody> {
        val mime = URLConnection.guessContentTypeFromName(file.name) ?: "image/jpeg"
        val filePart = MultipartBody.Part.createFormData(
            "file", file.name, file.asRequestBody(mime.toMediaTypeOrNull())
        )
        return apiInterface.callMyExpensefile(
            auth(),
            filePart,
            MultipartBody.Part.createFormData("docname", docName),
            MultipartBody.Part.createFormData("is_private", "0"),
            MultipartBody.Part.createFormData("folder", "Home/Attachments"),
            MultipartBody.Part.createFormData("doctype", "Expense Claim")
        )
    }
}
