package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.inventory.products.data.ProductsRepository
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductListResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSourceResponse
import com.axelliant.hris.features.quotes.data.importer.QuoteProductExcelParseResult
import com.axelliant.hris.features.quotes.data.importer.QuoteProductExcelRow
import com.axelliant.hris.features.quotes.data.importer.QuoteProductExcelSearchCandidate
import com.axelliant.hris.features.quotes.data.importer.QuoteProductExcelSearchType
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.domain.model.CreateDraftQuoteRequest
import com.axelliant.hris.features.quotes.domain.model.CreateDraftQuoteResult
import com.axelliant.hris.features.quotes.domain.model.EditQuoteDraftUi
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductScheduleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class AddQuoteViewModel @Inject constructor(
    private val repository: QuotesRepository,
    private val productsRepository: ProductsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val duplicateQuoteId: String? = savedStateHandle.get<String>(ARG_DUPLICATE_QUOTE_ID)
    private val reviseQuoteId: String? = savedStateHandle.get<String>(ARG_REVISE_QUOTE_ID)
    private val editQuoteId: String? = savedStateHandle.get<String>(ARG_QUOTE_ID)
        ?.takeIf { duplicateQuoteId.isNullOrBlank() && reviseQuoteId.isNullOrBlank() }
    private val quoteEntryMode: QuoteFormEntryMode = when {
        duplicateQuoteId?.isNotBlank() == true -> QuoteFormEntryMode.DUPLICATE
        reviseQuoteId?.isNotBlank() == true -> QuoteFormEntryMode.REVISE
        editQuoteId?.isNotBlank() == true -> QuoteFormEntryMode.EDIT
        else -> QuoteFormEntryMode.CREATE
    }
    private val customerSearchInput = MutableStateFlow("")
    private var customerSearchJob: Job? = null
    private var customerDetailsJob: Job? = null
    private var paymentTermsJob: Job? = null
    private var saveJob: Job? = null
    private var productImportJob: Job? = null

    private val _uiState = MutableStateFlow(AddQuoteUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<CreateDraftQuoteResult>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        observeCustomerSearch()
        when (quoteEntryMode) {
            QuoteFormEntryMode.DUPLICATE -> loadQuoteDraft(duplicateQuoteId!!, QuoteFormEntryMode.DUPLICATE)
            QuoteFormEntryMode.REVISE -> loadQuoteDraft(reviseQuoteId!!, QuoteFormEntryMode.REVISE)
            QuoteFormEntryMode.EDIT -> loadQuoteDraft(editQuoteId!!, QuoteFormEntryMode.EDIT)
            QuoteFormEntryMode.CREATE -> Unit
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomerSearch() {
        customerSearchInput
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach(::searchCustomers)
            .launchIn(viewModelScope)
    }

    fun onCustomerSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(customerSearchQuery = query)
        customerSearchInput.value = query
    }

    fun setQuoteTitle(title: String) {
        _uiState.value = _uiState.value.copy(
            quoteTitle = title,
            fieldErrors = _uiState.value.fieldErrors.withQuoteTitleValid(title.isNotBlank())
        )
    }

    fun selectCustomer(customer: QuoteCustomerUi) {
        customerDetailsJob?.cancel()
        paymentTermsJob?.cancel()

        _uiState.value = _uiState.value.copy(
            selectedCustomer = customer.copy(
                priceProfile = "",
                priceProfileId = "",
                creditHoldEnabled = null,
                remainingCredit = "",
                billingAddresses = emptyList(),
                shippingAddresses = emptyList()
            ),
            selectedBillingAddress = null,
            selectedShippingAddress = null,
            selectedPaymentTerm = null,
            paymentTerms = emptyList(),
            isCustomerDetailsLoading = true,
            isPaymentTermsLoading = false,
            errorMessage = null,
            fieldErrors = _uiState.value.fieldErrors.withCustomerValid(true)
        )
        loadCustomerDetails(customer)
    }

    fun selectPaymentTerm(term: QuotePaymentTermUi) {
        _uiState.value = _uiState.value.copy(
            selectedPaymentTerm = term,
            fieldErrors = _uiState.value.fieldErrors.withPaymentTermValid(true)
        )
    }

    fun selectBillingAddress(address: QuoteAddressUi) {
        _uiState.value = _uiState.value.copy(
            selectedBillingAddress = address,
            fieldErrors = _uiState.value.fieldErrors.withBillingAddressValid(true)
        )
    }

    fun selectShippingAddress(address: QuoteAddressUi) {
        val current = _uiState.value.copy(selectedShippingAddress = address)
        val updatedProducts = current.selectedProducts.map { product ->
            QuoteProductScheduleHelper.ensureSchedules(
                product = product,
                quoteShippingAddress = address,
                quoteDeliveryDate = current.deliveryDate
            )
        }
        _uiState.value = current.copy(
            selectedShippingAddress = address,
            selectedProducts = updatedProducts,
            fieldErrors = _uiState.value.fieldErrors.withShippingAddressValid(true)
        )
    }

    fun addAddress(type: AddressType, address: QuoteAddressUi) {
        val current = _uiState.value
        val customer = current.selectedCustomer ?: return
        val updatedCustomer = when (type) {
            AddressType.Billing -> customer.copy(billingAddresses = customer.billingAddresses + address)
            AddressType.Shipping -> customer.copy(shippingAddresses = customer.shippingAddresses + address)
        }
        val updatedProducts = if (type == AddressType.Shipping) {
            val shippingState = current.copy(
                selectedCustomer = updatedCustomer,
                selectedShippingAddress = address
            )
            current.selectedProducts.map { product ->
                QuoteProductScheduleHelper.ensureSchedules(
                    product = product,
                    quoteShippingAddress = address,
                    quoteDeliveryDate = shippingState.deliveryDate
                )
            }
        } else {
            current.selectedProducts
        }
        _uiState.value = current.copy(
            selectedCustomer = updatedCustomer,
            selectedBillingAddress = if (type == AddressType.Billing) address else current.selectedBillingAddress,
            selectedShippingAddress = if (type == AddressType.Shipping) address else current.selectedShippingAddress,
            selectedProducts = updatedProducts,
            fieldErrors = when (type) {
                AddressType.Billing -> _uiState.value.fieldErrors.withBillingAddressValid(true)
                AddressType.Shipping -> _uiState.value.fieldErrors.withShippingAddressValid(true)
            }
        )
    }

    fun setDeliveryDate(date: String) {
        val formatted = QuoteProductScheduleHelper.formatDeliveryDate(date)
        val current = _uiState.value.copy(deliveryDate = date)
        val updatedProducts = current.selectedProducts.map { product ->
            val ensured = QuoteProductScheduleHelper.ensureSchedules(
                product = product,
                quoteShippingAddress = current.selectedShippingAddress,
                quoteDeliveryDate = date
            )
            if (formatted.isBlank() || ensured.deliverySchedules.isEmpty()) {
                ensured
            } else {
                ensured.copy(
                    deliverySchedules = ensured.deliverySchedules.mapIndexed { index, schedule ->
                        if (index == 0) {
                            schedule.copy(estimatedDeliveryDate = formatted)
                        } else {
                            schedule
                        }
                    }
                )
            }
        }
        _uiState.value = current.copy(
            deliveryDate = date,
            selectedProducts = updatedProducts,
            fieldErrors = _uiState.value.fieldErrors.withDeliveryDateValid(date.isNotBlank())
        )
    }

    fun setProducts(products: List<QuoteCreationProductUi>) {
        val current = _uiState.value
        val mergedById = current.selectedProducts.associateBy { it.id }.toMutableMap()
        products.forEach { incoming ->
            mergedById[incoming.id] = mergedById[incoming.id] ?: incoming
        }
        val normalized = mergedById.values.map { product ->
            QuoteProductScheduleHelper.ensureSchedules(
                product = product,
                quoteShippingAddress = current.selectedShippingAddress,
                quoteDeliveryDate = current.deliveryDate
            )
        }
        _uiState.value = current.copy(
            selectedProducts = normalized,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(normalized.isNotEmpty())
        )
    }

    fun importProductsFromExcel(parseResult: QuoteProductExcelParseResult) {
        productImportJob?.cancel()
        if (parseResult.rows.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isProductImportLoading = false,
                errorMessage = "No valid product rows found in the Excel file."
            )
            return
        }

        productImportJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProductImportLoading = true,
                productImportPreview = null,
                errorMessage = null
            )

            val importedProducts = linkedMapOf<String, QuoteCreationProductUi>()
            var ignoredRows = parseResult.ignoredRows

            parseResult.rows.forEach { row ->
                val product = findProductForImportedRow(row)
                if (product == null) {
                    ignoredRows += 1
                } else {
                    importedProducts.putIfAbsent(product.id, product.copy(quantity = row.quantity))
                }
            }

            if (importedProducts.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isProductImportLoading = false,
                    productImportPreview = null,
                    errorMessage = "No matching products found on the server."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isProductImportLoading = false,
                productImportPreview = QuoteProductImportPreview(
                    id = System.currentTimeMillis(),
                    products = importedProducts.values.toList(),
                    ignoredRows = ignoredRows
                )
            )
        }
    }

    fun confirmImportedProducts(previewId: Long) {
        val current = _uiState.value
        val preview = current.productImportPreview?.takeIf { it.id == previewId } ?: return
        val products = mergeProducts(
            existing = current.selectedProducts,
            incoming = preview.products
        )
        _uiState.value = current.copy(
            selectedProducts = products,
            productImportPreview = null,
            fieldErrors = current.fieldErrors.withProductsValid(products.isNotEmpty())
        )
    }

    fun dismissProductImportPreview() {
        _uiState.value = _uiState.value.copy(productImportPreview = null)
    }

    fun updateProduct(product: QuoteCreationProductUi) {
        val products = _uiState.value.selectedProducts.map { existing ->
            if (existing.id == product.id) product else existing
        }
        _uiState.value = _uiState.value.copy(
            selectedProducts = products,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(products.isNotEmpty())
        )
    }

    fun removeProduct(productId: String) {
        val products = _uiState.value.selectedProducts.filter { it.id != productId }
        _uiState.value = _uiState.value.copy(
            selectedProducts = products,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(products.isNotEmpty())
        )
    }

    fun setDealRegistration(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dealRegistration = enabled)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun onValidationScrollHandled() {
        _uiState.value = _uiState.value.copy(scrollToFirstValidationError = false)
    }

    fun saveDraft() {
        val fieldErrors = validateDraft()
        if (fieldErrors.hasAny()) {
            _uiState.value = _uiState.value.copy(
                fieldErrors = fieldErrors,
                scrollToFirstValidationError = true
            )
            _saveState.value = UiState.Idle
            return
        }

        val current = _uiState.value.copy(
            fieldErrors = AddQuoteFieldErrors(),
            scrollToFirstValidationError = false
        )
        _uiState.value = current
        val customer = current.selectedCustomer ?: return
        val billingAddress = current.selectedBillingAddress ?: return
        val shippingAddress = current.selectedShippingAddress ?: return
        val paymentTerm = current.selectedPaymentTerm ?: return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _saveState.value = UiState.Loading
            when (
                val result = repository.createDraftQuote(
                    CreateDraftQuoteRequest(
                        quoteId = current.editQuoteId,
                        quoteTitle = current.quoteTitle.trim(),
                        customer = customer,
                        billingAddress = billingAddress,
                        shippingAddress = shippingAddress,
                        paymentTerm = paymentTerm,
                        deliveryDate = current.deliveryDate,
                        products = current.selectedProducts,
                        dealRegistration = current.dealRegistration
                    )
                )
            ) {
                is ApiResult.Success -> _saveState.value = UiState.Success(result.data)
                is ApiResult.HttpError -> _saveState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> _saveState.value = UiState.Error(result.message)
                is ApiResult.UnknownError -> _saveState.value = UiState.Error(result.message)
                ApiResult.Unauthorized -> _saveState.value = UiState.Unauthorized
                ApiResult.Empty -> _saveState.value = UiState.Error("Unable to save quote draft.")
            }
        }
    }

    private fun searchCustomers(query: String) {
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCustomerSearchLoading = true,
                errorMessage = null
            )
            when (val result = repository.searchCustomers(query)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        customers = result.data,
                        isCustomerSearchLoading = false
                    )
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        customers = emptyList(),
                        isCustomerSearchLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        customers = emptyList(),
                        isCustomerSearchLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        customers = emptyList(),
                        isCustomerSearchLoading = false,
                        errorMessage = result.message
                    )
                }
                ApiResult.Unauthorized -> {
                    _uiState.value = _uiState.value.copy(
                        customers = emptyList(),
                        isCustomerSearchLoading = false
                    )
                    _saveState.value = UiState.Unauthorized
                }
                ApiResult.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        customers = emptyList(),
                        isCustomerSearchLoading = false
                    )
                }
            }
        }
    }

    private fun loadCustomerDetails(customer: QuoteCustomerUi) {
        customerDetailsJob = viewModelScope.launch {
            when (val result = repository.getCustomerDetailsForQuotation(customer)) {
                is ApiResult.Success -> {
                    val enriched = result.data
                    val shippingAddress = enriched.shippingAddresses.firstOrNull()
                    val deliveryDate = _uiState.value.deliveryDate
                    val updatedProducts = _uiState.value.selectedProducts.map { product ->
                        QuoteProductScheduleHelper.ensureSchedules(
                            product = product,
                            quoteShippingAddress = shippingAddress,
                            quoteDeliveryDate = deliveryDate
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        selectedCustomer = enriched,
                        selectedBillingAddress = enriched.billingAddresses.firstOrNull(),
                        selectedShippingAddress = shippingAddress,
                        selectedProducts = updatedProducts,
                        isCustomerDetailsLoading = false,
                        fieldErrors = _uiState.value.fieldErrors
                            .withCustomerValid(true)
                            .withBillingAddressValid(enriched.billingAddresses.isNotEmpty())
                            .withShippingAddressValid(shippingAddress != null)
                    )
                    loadPaymentTerms(enriched.id)
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        isCustomerDetailsLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isCustomerDetailsLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        isCustomerDetailsLoading = false,
                        errorMessage = result.message
                    )
                }
                ApiResult.Unauthorized -> {
                    _uiState.value = _uiState.value.copy(isCustomerDetailsLoading = false)
                    _saveState.value = UiState.Unauthorized
                }
                ApiResult.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        isCustomerDetailsLoading = false,
                        errorMessage = "Customer details not found."
                    )
                }
            }
        }
    }

    private fun loadPaymentTerms(accountId: String) {
        paymentTermsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPaymentTermsLoading = true)
            when (val result = repository.getPaymentTermsForQuotation(accountId)) {
                is ApiResult.Success -> {
                    val terms = result.data
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = terms,
                        selectedPaymentTerm = _uiState.value.selectedPaymentTerm?.let { selected ->
                            terms.firstOrNull { it.id == selected.id } ?: selected
                        } ?: terms.firstOrNull(),
                        isPaymentTermsLoading = false,
                        fieldErrors = _uiState.value.fieldErrors.withPaymentTermValid(terms.isNotEmpty())
                    )
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = emptyList(),
                        isPaymentTermsLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = emptyList(),
                        isPaymentTermsLoading = false,
                        errorMessage = result.message
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = emptyList(),
                        isPaymentTermsLoading = false,
                        errorMessage = result.message
                    )
                }
                ApiResult.Unauthorized -> {
                    _uiState.value = _uiState.value.copy(isPaymentTermsLoading = false)
                    _saveState.value = UiState.Unauthorized
                }
                ApiResult.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = emptyList(),
                        isPaymentTermsLoading = false
                    )
                }
            }
        }
    }

    private fun validateDraft(): AddQuoteFieldErrors {
        val state = _uiState.value
        return AddQuoteFieldErrors(
            quoteTitle = state.quoteTitle.isBlank(),
            customer = state.selectedCustomer == null,
            billingAddress = state.selectedBillingAddress == null,
            shippingAddress = state.selectedShippingAddress == null,
            paymentTerm = state.selectedPaymentTerm == null,
            deliveryDate = state.deliveryDate.isBlank(),
            products = state.selectedProducts.isEmpty()
        )
    }

    private fun loadQuoteDraft(quoteId: String, mode: QuoteFormEntryMode) {
        val isDuplicate = mode == QuoteFormEntryMode.DUPLICATE
        val isRevise = mode == QuoteFormEntryMode.REVISE
        val isEditableExistingQuote = mode == QuoteFormEntryMode.EDIT || isRevise
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                editQuoteId = if (isDuplicate) null else quoteId,
                isEditMode = isEditableExistingQuote,
                isReviseMode = isRevise,
                isDuplicateMode = isDuplicate,
                isEditLoading = true,
                isEditLoadFailed = false,
                errorMessage = null
            )
            when (val result = repository.getQuoteForEdit(quoteId)) {
                is ApiResult.Success -> {
                    val draft = enrichDraftFromCustomerDetails(result.data)
                    val products = if (isDuplicate) {
                        draft.products.map { product -> product.copy(lineItemId = null) }
                    } else {
                        draft.products
                    }
                    _uiState.value = _uiState.value.copy(
                        editQuoteId = if (isDuplicate) null else draft.quoteId,
                        isEditMode = isEditableExistingQuote,
                        isReviseMode = isRevise,
                        isDuplicateMode = isDuplicate,
                        isEditLoading = false,
                        isEditLoadFailed = false,
                        quoteTitle = draft.quoteTitle,
                        selectedCustomer = draft.customer,
                        selectedBillingAddress = draft.billingAddress,
                        selectedShippingAddress = draft.shippingAddress,
                        selectedPaymentTerm = draft.paymentTerm,
                        paymentTerms = listOf(draft.paymentTerm),
                        deliveryDate = draft.deliveryDate,
                        selectedProducts = products,
                        dealRegistration = draft.dealRegistration,
                        fieldErrors = AddQuoteFieldErrors()
                    )
                    loadPaymentTerms(draft.customer.id)
                }
                is ApiResult.HttpError -> {
                    _uiState.value = _uiState.value.copy(
                        isEditLoading = false,
                        isEditLoadFailed = true,
                        errorMessage = result.message
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isEditLoading = false,
                        isEditLoadFailed = true,
                        errorMessage = result.message
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = _uiState.value.copy(
                        isEditLoading = false,
                        isEditLoadFailed = true,
                        errorMessage = result.message
                    )
                }
                ApiResult.Unauthorized -> {
                    _uiState.value = _uiState.value.copy(
                        isEditLoading = false,
                        isEditLoadFailed = true
                    )
                    _saveState.value = UiState.Unauthorized
                }
                ApiResult.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        isEditLoading = false,
                        isEditLoadFailed = true,
                        errorMessage = "Quote details not found."
                    )
                }
            }
        }
    }

    private suspend fun enrichDraftFromCustomerDetails(draft: EditQuoteDraftUi): EditQuoteDraftUi {
        return when (val result = repository.getCustomerDetailsForQuotation(draft.customer)) {
            is ApiResult.Success -> {
                val enrichedCustomer = result.data.copy(
                    priceProfileId = draft.customer.priceProfileId.ifBlank { result.data.priceProfileId },
                    priceProfile = draft.customer.priceProfile.ifBlank { result.data.priceProfile },
                    creditHoldEnabled = draft.customer.creditHoldEnabled ?: result.data.creditHoldEnabled,
                    remainingCredit = draft.customer.remainingCredit.ifBlank { result.data.remainingCredit },
                    email = draft.customer.email.ifBlank { result.data.email },
                    accountExecutive = draft.customer.accountExecutive.ifBlank { result.data.accountExecutive }
                )
                val billingAddress = resolveAddress(
                    preferred = draft.billingAddress,
                    options = enrichedCustomer.billingAddresses
                ) ?: draft.billingAddress
                val shippingAddress = resolveAddress(
                    preferred = draft.shippingAddress,
                    options = enrichedCustomer.shippingAddresses
                ) ?: draft.shippingAddress
                draft.copy(
                    customer = enrichedCustomer.copy(
                        billingAddresses = enrichedCustomer.billingAddresses.ifEmpty {
                            listOf(billingAddress)
                        },
                        shippingAddresses = enrichedCustomer.shippingAddresses.ifEmpty {
                            listOf(shippingAddress)
                        }
                    ),
                    billingAddress = billingAddress,
                    shippingAddress = shippingAddress
                )
            }
            else -> draft
        }
    }

    private suspend fun findProductForImportedRow(row: QuoteProductExcelRow): QuoteCreationProductUi? {
        row.searchCandidates.forEach { candidate ->
            val result = productsRepository.getProducts(
                search = candidate.value,
                status = ACTIVE_PRODUCT_STATUS,
                limit = PRODUCT_IMPORT_SEARCH_LIMIT,
                includeNameInSearchPayload = false
            )
            if (result is ApiResult.Success) {
                val matched = result.data.sourceProducts()
                    .firstOrNull { source -> source.matchesImportedSearch(candidate) }
                if (matched != null) {
                    return matched.toQuoteCreationProduct(row.quantity)
                }
            }
        }
        return null
    }

    private fun ProductListResponse.sourceProducts(): List<ProductSourceResponse> {
        return products.takeIf { it.isNotEmpty() || hits == null }
            ?: hits?.hits.orEmpty().mapNotNull { it.source }
    }

    private fun ProductSourceResponse.matchesImportedSearch(candidate: QuoteProductExcelSearchCandidate): Boolean {
        return when (candidate.type) {
            QuoteProductExcelSearchType.ManufacturerPartNumber ->
                manufacturerPartNumber.equals(candidate.value, ignoreCase = true)
            QuoteProductExcelSearchType.Sku ->
                axePartNumber.equals(candidate.value, ignoreCase = true)
        }
    }

    private fun ProductSourceResponse.toQuoteCreationProduct(quantity: Int): QuoteCreationProductUi {
        val vendor = vendorInfo.orEmpty().firstOrNull()
        val price = vendor?.listPrice ?: listPrice ?: 0.0
        val resolvedName = name.orEmpty().ifBlank { "Unnamed Product" }
        val thumbnail = buildThumbnailLabel()
        return QuoteCreationProductUi(
            id = id.orEmpty(),
            name = resolvedName,
            sku = axePartNumber.orEmpty(),
            category = category?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
            thumbnailLabel = thumbnail,
            brandThumbnail = thumbnail == CISCO_LABEL,
            unitPrice = price,
            quantity = quantity
        )
    }

    private fun ProductSourceResponse.buildThumbnailLabel(): String {
        val text = listOfNotNull(name, description, manufacturerName)
            .joinToString(" ")
            .uppercase(Locale.US)

        if (CISCO_LABEL in text) return CISCO_LABEL

        return manufacturerName
            ?.take(3)
            ?.uppercase(Locale.US)
            ?.ifBlank { null }
            ?: name.orEmpty().take(3).uppercase(Locale.US).ifBlank { "APP" }
    }

    private fun mergeProducts(
        existing: List<QuoteCreationProductUi>,
        incoming: List<QuoteCreationProductUi>
    ): List<QuoteCreationProductUi> {
        val existingById = existing.associateBy { it.id }
        return existing + incoming.filterNot { imported -> imported.id in existingById }
    }

    private fun resolveAddress(
        preferred: QuoteAddressUi,
        options: List<QuoteAddressUi>
    ): QuoteAddressUi? {
        val matched = options.find { it.id == preferred.id }
        if (matched != null) return matched
        if (preferred.address.isNotBlank()) return preferred
        return options.firstOrNull()
    }

    companion object {
        const val RESULT_QUOTE_CREATED = "quoteCreated"
        const val RESULT_ADDRESS = "quoteAddress"
        const val RESULT_PRODUCTS = "quoteProducts"
        const val ARG_ADDRESS_TYPE = "addressType"
        const val ARG_QUOTE_ID = "quoteId"
        const val ARG_DUPLICATE_QUOTE_ID = "duplicateQuoteId"
        const val ARG_REVISE_QUOTE_ID = "reviseQuoteId"
        const val ADDRESS_TYPE_BILLING = "billing"
        const val ADDRESS_TYPE_SHIPPING = "shipping"

        const val VALIDATION_QUOTE_TITLE = "validation-quote-title"
        const val VALIDATION_CUSTOMER = "validation-customer"
        const val VALIDATION_PRICE_PROFILE = "validation-price-profile"
        const val VALIDATION_BILLING_ADDRESS = "validation-billing-address"
        const val VALIDATION_SHIPPING_ADDRESS = "validation-shipping-address"
        const val VALIDATION_PAYMENT_TERM = "validation-payment-term"
        const val VALIDATION_DELIVERY_DATE = "validation-delivery-date"
        const val VALIDATION_PRODUCTS = "validation-products"

        private const val SEARCH_DEBOUNCE_MS = 400L
        private const val ACTIVE_PRODUCT_STATUS = 1
        private const val PRODUCT_IMPORT_SEARCH_LIMIT = 10
        private const val CISCO_LABEL = "CISCO"
    }
}

private enum class QuoteFormEntryMode {
    CREATE,
    EDIT,
    DUPLICATE,
    REVISE
}

data class AddQuoteFieldErrors(
    val quoteTitle: Boolean = false,
    val customer: Boolean = false,
    val billingAddress: Boolean = false,
    val shippingAddress: Boolean = false,
    val paymentTerm: Boolean = false,
    val deliveryDate: Boolean = false,
    val products: Boolean = false
) {
    fun hasAny(): Boolean {
        return quoteTitle || customer || billingAddress || shippingAddress ||
            paymentTerm || deliveryDate || products
    }

    fun withQuoteTitleValid(isValid: Boolean) = if (isValid) copy(quoteTitle = false) else this
    fun withCustomerValid(isValid: Boolean) = if (isValid) copy(customer = false) else this
    fun withBillingAddressValid(isValid: Boolean) = if (isValid) copy(billingAddress = false) else this
    fun withShippingAddressValid(isValid: Boolean) = if (isValid) copy(shippingAddress = false) else this
    fun withPaymentTermValid(isValid: Boolean) = if (isValid) copy(paymentTerm = false) else this
    fun withDeliveryDateValid(isValid: Boolean) = if (isValid) copy(deliveryDate = false) else this
    fun withProductsValid(isValid: Boolean) = if (isValid) copy(products = false) else this
}

data class AddQuoteUiState(
    val editQuoteId: String? = null,
    val isEditMode: Boolean = false,
    val isReviseMode: Boolean = false,
    val isDuplicateMode: Boolean = false,
    val isEditLoadFailed: Boolean = false,
    val quoteTitle: String = "",
    val customerSearchQuery: String = "",
    val customers: List<QuoteCustomerUi> = emptyList(),
    val paymentTerms: List<QuotePaymentTermUi> = emptyList(),
    val selectedCustomer: QuoteCustomerUi? = null,
    val selectedBillingAddress: QuoteAddressUi? = null,
    val selectedShippingAddress: QuoteAddressUi? = null,
    val selectedPaymentTerm: QuotePaymentTermUi? = null,
    val deliveryDate: String = "",
    val selectedProducts: List<QuoteCreationProductUi> = emptyList(),
    val dealRegistration: Boolean = false,
    val isCustomerSearchLoading: Boolean = false,
    val isCustomerDetailsLoading: Boolean = false,
    val isPaymentTermsLoading: Boolean = false,
    val isEditLoading: Boolean = false,
    val isProductImportLoading: Boolean = false,
    val productImportPreview: QuoteProductImportPreview? = null,
    val errorMessage: String? = null,
    val fieldErrors: AddQuoteFieldErrors = AddQuoteFieldErrors(),
    val scrollToFirstValidationError: Boolean = false
) {
    val isFormLoading: Boolean
        get() = isCustomerDetailsLoading || isPaymentTermsLoading || isEditLoading ||
            isProductImportLoading

    val subtotal: Double
        get() = selectedProducts.sumOf { it.lineTotal }

    val tax: Double = 0.0
    val shipping: Double = 0.0
    val grandTotal: Double
        get() = subtotal + tax + shipping

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }
}

data class QuoteProductImportPreview(
    val id: Long,
    val products: List<QuoteCreationProductUi>,
    val ignoredRows: Int
)

enum class AddressType {
    Billing,
    Shipping;

    val resultKey: String
        get() = when (this) {
            Billing -> "${AddQuoteViewModel.RESULT_ADDRESS}_${AddQuoteViewModel.ADDRESS_TYPE_BILLING}"
            Shipping -> "${AddQuoteViewModel.RESULT_ADDRESS}_${AddQuoteViewModel.ADDRESS_TYPE_SHIPPING}"
        }

    val navValue: String
        get() = when (this) {
            Billing -> AddQuoteViewModel.ADDRESS_TYPE_BILLING
            Shipping -> AddQuoteViewModel.ADDRESS_TYPE_SHIPPING
        }

    companion object {
        fun fromNavValue(value: String?): AddressType {
            return if (value == AddQuoteViewModel.ADDRESS_TYPE_SHIPPING) Shipping else Billing
        }
    }
}
