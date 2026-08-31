package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.quotes.data.remote.CreateQuoteApiService
import com.axelliant.hris.features.quotes.data.remote.QuotesApiService
import com.axelliant.hris.features.quotes.data.remote.WorkflowApiService
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationRequest
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationResponse
import com.axelliant.hris.features.quotes.data.remote.dto.QuotePreviewResponse
import com.axelliant.hris.features.profiles.data.local.ProfileSettingsStore
import com.axelliant.hris.features.quotes.domain.model.CancelQuoteResult
import com.axelliant.hris.features.quotes.domain.model.CreateDraftQuoteResult
import com.axelliant.hris.features.quotes.domain.model.CreateDraftQuoteRequest
import com.axelliant.hris.features.quotes.domain.model.SubmitQuoteResult
import com.axelliant.hris.features.quotes.domain.model.EditQuoteDraftUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.axelliant.hris.features.quotes.domain.model.QuoteType
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class QuotesRepository @Inject constructor(
    private val apiService: QuotesApiService,
    private val createQuoteApiService: CreateQuoteApiService,
    private val workflowApiService: WorkflowApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val profileSettingsStore: ProfileSettingsStore
) {
    private val quoteCache = mutableMapOf<String, QuoteModel>()
    private val localDraftQuotes = mutableListOf<QuoteModel>()

    suspend fun getAllQuotes(request: GetQuotationRequest): ApiResult<QuoteListPage> = withContext(Dispatchers.IO) {
        when (val result = safeApiExecutor.execute { apiService.getAllQuotes(request) }) {
            is ApiResult.Success -> mapListResponse(result.data, request)
            is ApiResult.Empty -> ApiResult.UnknownError("No quotes found.")
            is ApiResult.HttpError -> ApiResult.HttpError(result.code, result.message)
            is ApiResult.NetworkError -> ApiResult.NetworkError(result.message)
            is ApiResult.UnknownError -> ApiResult.UnknownError(result.message)
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getQuotePreview(quoteId: String): ApiResult<QuotePreviewUiModel> {
        return when (val result = safeApiExecutor.execute { apiService.getQuotePreview(quoteId) }) {
            is ApiResult.Success -> mapPreviewResponse(result.data)
            is ApiResult.Empty -> ApiResult.UnknownError("Quote preview not found.")
            is ApiResult.HttpError -> ApiResult.HttpError(result.code, result.message)
            is ApiResult.NetworkError -> ApiResult.NetworkError(result.message)
            is ApiResult.UnknownError -> ApiResult.UnknownError(result.message)
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getQuoteForEdit(quoteId: String): ApiResult<EditQuoteDraftUi> {
        return when (val result = safeApiExecutor.execute { apiService.getQuoteSingle(quoteId) }) {
            is ApiResult.Success -> {
                val payload = result.data
                val response = QuoteApiResponseParser.unwrapSuccess(payload)
                val editDraft = response?.let {
                    QuoteCreationMapper.toEditDraft(
                        response = it,
                        fallbackQuoteId = quoteId
                    )
                }
                if (editDraft == null) {
                    val apiMessage = payload.data?.message?.takeIf { it.isNotBlank() }
                    ApiResult.UnknownError(
                        apiMessage ?: "Unable to load quote for editing."
                    )
                } else {
                    ApiResult.Success(editDraft)
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Quote details not found.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getQuoteById(quoteId: String): QuoteModel? = quoteCache[quoteId]

    fun cacheQuotes(quotes: List<QuoteModel>) {
        quotes.forEach { quote -> quoteCache[quote.id] = quote }
    }

    suspend fun saveQuote(quote: QuoteModel): QuoteModel {
        quoteCache[quote.id] = quote
        return quote
    }

    suspend fun searchCustomers(
        search: String,
        quoteType: Int = QUOTE_TYPE_STANDARD
    ): ApiResult<List<QuoteCustomerUi>> {
        return when (val result = safeApiExecutor.execute {
            createQuoteApiService.getAllAccountsDdl(search = search, quoteType = quoteType)
        }) {
            is ApiResult.Success -> {
                val customers = QuoteApiResponseParser.unwrapSuccess(result.data)
                    .orEmpty()
                    .mapNotNull(QuoteCreationMapper::mapAccount)
                ApiResult.Success(customers)
            }
            is ApiResult.Empty -> ApiResult.Success(emptyList())
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getCustomerDetailsForQuotation(
        customer: QuoteCustomerUi
    ): ApiResult<QuoteCustomerUi> {
        return when (val result = safeApiExecutor.execute {
            createQuoteApiService.getCustomerDetailsForQuotation(accountId = customer.id)
        }) {
            is ApiResult.Success -> {
                val details = QuoteApiResponseParser.unwrapSuccess(result.data)
                if (details == null) {
                    val message = QuoteApiResponseParser.extractApiMessage(result.data)
                        ?: "Unable to load customer details."
                    ApiResult.UnknownError(message)
                } else {
                    ApiResult.Success(QuoteCreationMapper.applyCustomerDetails(customer, details))
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Customer details not found.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getPaymentTermsForQuotation(
        accountId: String,
        type: Int = PAYMENT_TERM_TYPE_STANDARD
    ): ApiResult<List<QuotePaymentTermUi>> {
        return when (val result = safeApiExecutor.execute {
            createQuoteApiService.getPaymentTermsForDdl(type = type, accountId = accountId)
        }) {
            is ApiResult.Success -> {
                val terms = QuoteApiResponseParser.unwrapSuccess(result.data)
                    .orEmpty()
                    .mapNotNull(QuoteCreationMapper::mapPaymentTerm)
                ApiResult.Success(terms)
            }
            is ApiResult.Empty -> ApiResult.Success(emptyList())
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getQuoteWorkflow(
        relationId: String,
        quoteNumber: String
    ): ApiResult<QuoteWorkflowUiModel> {

        return when (
            val result = safeApiExecutor.execute {
                workflowApiService.getWorkflowGraphInstance(relationId)
            }
        ) {

            is ApiResult.Success -> {

                val payload = result.data

                val workflow = QuoteApiResponseParser.unwrapSuccess(
                    payload
                )

                if (payload.data?.success == true && workflow != null) {

                    ApiResult.Success(
                        QuoteWorkflowGraphMapper.toUiModel(
                            quoteNumber = quoteNumber,
                            workflow = workflow
                        )
                    )

                } else {

                    ApiResult.UnknownError(
                        QuoteApiResponseParser.extractApiMessage(payload)
                            ?: "Unable to load quote workflow."
                    )
                }
            }

            is ApiResult.Empty ->
                ApiResult.UnknownError(
                    "Unable to load quote workflow."
                )

            is ApiResult.HttpError ->
                ApiResult.HttpError(
                    code = result.code,
                    message = QuoteApiResponseParser.resolveErrorMessage(result),
                    errorBody = result.errorBody
                )

            is ApiResult.NetworkError ->
                result

            is ApiResult.UnknownError ->
                result

            ApiResult.Unauthorized ->
                ApiResult.Unauthorized
        }
    }

    suspend fun submitQuote(quoteId: String): ApiResult<SubmitQuoteResult> {
        return when (val result = safeApiExecutor.execute { apiService.submitQuote(quoteId) }) {
            is ApiResult.Success -> {
                val payload = result.data
                val serverMessage = payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: QuoteApiResponseParser.extractApiMessage(payload)
                if (payload.data?.success == true) {
                    ApiResult.Success(
                        SubmitQuoteResult(
                            message = serverMessage.orEmpty().ifBlank { "Quote submitted successfully." }
                        )
                    )
                } else {
                    ApiResult.UnknownError(
                        serverMessage ?: "Quote submission failed."
                    )
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Quote submission failed.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun cancelQuote(quoteId: String): ApiResult<CancelQuoteResult> {
        return when (val result = safeApiExecutor.execute { apiService.cancelQuote(quoteId) }) {
            is ApiResult.Success -> {
                val payload = result.data
                val serverMessage = payload.data?.message?.takeIf { it.isNotBlank() }
                if (payload.data?.success == true) {
                    ApiResult.Success(
                        CancelQuoteResult(
                            message = serverMessage ?: "Quote cancelled successfully."
                        )
                    )
                } else {
                    ApiResult.UnknownError(
                        serverMessage ?: "Unable to cancel quote."
                    )
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Unable to cancel quote.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun createDraftQuote(request: CreateDraftQuoteRequest): ApiResult<CreateDraftQuoteResult> {
        val apiRequest = QuoteCreationMapper.toAddEditRequest(
            quoteTitle = request.quoteTitle,
            draft = request
        )
        return when (val result = safeApiExecutor.execute {
            createQuoteApiService.addEditQuotation(apiRequest)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val response = QuoteApiResponseParser.extractAddEditQuotationResponse(payload)
                val serverMessage = QuoteApiResponseParser.extractApiMessage(payload)
                if (payload.data?.success != true) {
                    ApiResult.UnknownError(serverMessage ?: "Unable to save quote draft.")
                } else {
                    val createdAt = Date()
                    val quote = QuoteModel(
                        id = response?.id.orEmpty().ifBlank { "remote-${createdAt.time}" },
                        quoteId = response?.quoteSerialNo
                            ?: response?.quotationId
                            ?: request.quoteTitle,
                        customerName = request.customer.name,
                        approvalStatus = QuoteStatus.Draft.apiValue,
                        createdBy = request.customer.accountExecutive,
                        date = request.deliveryDate.ifBlank {
                            SimpleDateFormat("MM-dd-yyyy", Locale.US).format(createdAt)
                        },
                        totalAmount = NumberFormat.getCurrencyInstance(Locale.US)
                            .format(request.products.sumOf { it.lineTotal }),
                        quoteType = QuoteType.Standard,
                        paymentTerm = request.paymentTerm.name,
                        notes = request.quoteTitle
                    )
                    quoteCache[quote.id] = quote
                    ApiResult.Success(
                        CreateDraftQuoteResult(
                            quote = quote,
                            message = serverMessage.orEmpty().ifBlank { "Quote saved as draft." }
                        )
                    )
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Unable to save quote draft.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = QuoteApiResponseParser.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun mapListResponse(
        response: BaseApiModel<GetQuotationResponse>,
        request: GetQuotationRequest
    ): ApiResult<QuoteListPage> {
        val payload = response.data
        val body = payload?.data
        return if (payload?.success == true && body != null) {
            val quoteType = request.quoteType.toQuoteType()
            val page = QuoteListMapper.mapPage(body, quoteType)
            val matchingLocalDrafts = localDraftQuotes.filter { draft ->
                draft.quoteType == quoteType &&
                    request.start == 0 &&
                    (request.approvalStatus == null || request.approvalStatus == QuoteStatus.Draft.apiValue) &&
                    draft.matchesSearch(request.search)
            }
            val mergedPage = page.copy(
                quotes = matchingLocalDrafts + page.quotes.filterNot { quote ->
                    matchingLocalDrafts.any { it.id == quote.id }
                },
                totalCount = page.totalCount + matchingLocalDrafts.size,
                approvalStatusCounts = page.approvalStatusCounts?.copy(
                    saveAsDraft = (page.approvalStatusCounts.saveAsDraft ?: 0) + matchingLocalDrafts.size
                )
            )
            cacheQuotes(mergedPage.quotes)
            ApiResult.Success(mergedPage)
        } else {
            ApiResult.UnknownError(
                response.message?.text.orEmpty().ifBlank { "Unable to load quotes." }
            )
        }
    }

    private fun mapPreviewResponse(
        response: BaseApiModel<QuotePreviewResponse>
    ): ApiResult<QuotePreviewUiModel> {
        val payload = response.data
        val preview = payload?.data
        return if (payload?.success == true && preview != null) {
            ApiResult.Success(QuotePreviewMapper.toUiModel(preview))
        } else {
            ApiResult.UnknownError(
                response.message?.text.orEmpty().ifBlank { "Unable to load quote preview." }
            )
        }
    }

    private fun Int.toQuoteType(): QuoteType {
        return if (this == QUOTE_TYPE_QUICK) QuoteType.Quick else QuoteType.Standard
    }

    private fun QuoteModel.matchesSearch(search: String): Boolean {
        val trimmed = search.trim()
        if (trimmed.isEmpty()) return true
        return quoteId.contains(trimmed, ignoreCase = true) ||
            customerName.contains(trimmed, ignoreCase = true) ||
            createdBy.contains(trimmed, ignoreCase = true)
    }

    companion object {
        const val QUOTE_TYPE_STANDARD = 1
        const val QUOTE_TYPE_QUICK = 2
        const val PAYMENT_TERM_TYPE_STANDARD = 1
        const val PAGE_SIZE = 10
    }
}
