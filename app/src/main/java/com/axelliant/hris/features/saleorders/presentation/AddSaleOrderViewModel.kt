package com.axelliant.hris.features.saleorders.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.R
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductScheduleHelper
import com.axelliant.hris.features.quotes.presentation.AddressType
import com.axelliant.hris.features.saleorders.data.SaleOrdersRepository
import com.axelliant.hris.features.saleorders.domain.model.EditSaleOrderDraftUi
import com.axelliant.hris.features.saleorders.domain.model.SaveSaleOrderRequest
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatus
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
class AddSaleOrderViewModel @Inject constructor(
    private val quotesRepository: QuotesRepository,
    private val saleOrdersRepository: SaleOrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editSaleOrderId: String? = savedStateHandle.get<String>(ARG_SALE_ORDER_ID)
        ?.takeIf { it.isNotBlank() }

    private val customerSearchInput = MutableStateFlow("")
    private var customerSearchJob: Job? = null
    private var customerDetailsJob: Job? = null
    private var paymentTermsJob: Job? = null

    private val _uiState = MutableStateFlow(
        AddSaleOrderUiState(
            shippingMethods = SaleOrderShippingMethod.defaults(),
            selectedShippingMethod = SaleOrderShippingMethod.defaults().firstOrNull(),
            editSaleOrderId = editSaleOrderId,
            isEditMode = editSaleOrderId != null
        )
    )
    val uiState = _uiState.asStateFlow()

    private var originalEditFingerprint: String? = null

    private val _saveState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        observeCustomerSearch()
        if (editSaleOrderId != null) {
            loadSaleOrderDraft(editSaleOrderId)
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
        ).withDirtyState()
        loadCustomerDetails(customer)
    }

    fun selectPaymentTerm(term: QuotePaymentTermUi) {
        _uiState.value = _uiState.value.copy(
            selectedPaymentTerm = term,
            fieldErrors = _uiState.value.fieldErrors.withPaymentTermValid(true)
        ).withDirtyState()
    }

    fun selectBillingAddress(address: QuoteAddressUi) {
        _uiState.value = _uiState.value.copy(
            selectedBillingAddress = address,
            fieldErrors = _uiState.value.fieldErrors.withBillingAddressValid(true)
        ).withDirtyState()
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
        ).withDirtyState()
    }

    fun addAddress(type: AddressType, address: QuoteAddressUi) {
        val current = _uiState.value
        val customer = current.selectedCustomer ?: return
        val updatedCustomer = when (type) {
            AddressType.Billing -> customer.copy(billingAddresses = customer.billingAddresses + address)
            AddressType.Shipping -> customer.copy(shippingAddresses = customer.shippingAddresses + address)
        }
        val updatedProducts = if (type == AddressType.Shipping) {
            current.selectedProducts.map { product ->
                QuoteProductScheduleHelper.ensureSchedules(
                    product = product,
                    quoteShippingAddress = address,
                    quoteDeliveryDate = current.deliveryDate
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
        ).withDirtyState()
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
        ).withDirtyState()
    }

    fun selectShippingMethod(method: SaleOrderShippingMethod) {
        _uiState.value = _uiState.value.copy(
            selectedShippingMethod = method,
            fieldErrors = _uiState.value.fieldErrors.withShippingMethodValid(true)
        ).withDirtyState()
    }

    fun setProducts(products: List<QuoteCreationProductUi>) {
        val current = _uiState.value
        val existingById = current.selectedProducts.associateBy { it.id }
        val merged = products.map { incoming -> existingById[incoming.id] ?: incoming }
        val normalized = merged.map { product ->
            QuoteProductScheduleHelper.ensureSchedules(
                product = product,
                quoteShippingAddress = current.selectedShippingAddress,
                quoteDeliveryDate = current.deliveryDate
            )
        }
        _uiState.value = current.copy(
            selectedProducts = normalized,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(normalized.isNotEmpty())
        ).withDirtyState()
    }

    fun updateProduct(product: QuoteCreationProductUi) {
        val products = _uiState.value.selectedProducts.map { existing ->
            if (existing.id == product.id) product else existing
        }
        _uiState.value = _uiState.value.copy(
            selectedProducts = products,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(products.isNotEmpty())
        ).withDirtyState()
    }

    fun removeProduct(productId: String) {
        val products = _uiState.value.selectedProducts.filter { it.id != productId }
        _uiState.value = _uiState.value.copy(
            selectedProducts = products,
            fieldErrors = _uiState.value.fieldErrors.withProductsValid(products.isNotEmpty())
        ).withDirtyState()
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
        val request = buildSaveRequest() ?: run {
            _saveState.value = UiState.Error("Complete all required fields before saving.")
            return
        }
        _uiState.value = _uiState.value.copy(
            fieldErrors = AddSaleOrderFieldErrors(),
            scrollToFirstValidationError = false
        )
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = when (val result = saleOrdersRepository.saveSaleOrder(request)) {
                is ApiResult.Success -> UiState.Success(result.data.message)
                is ApiResult.Empty -> UiState.Error("Unable to save sale order.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun onSaveHandled() {
        _saveState.value = UiState.Idle
    }

    private fun searchCustomers(query: String) {
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCustomerSearchLoading = true,
                errorMessage = null
            )
            when (val result = quotesRepository.searchCustomers(query)) {
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
            when (val result = quotesRepository.getCustomerDetailsForQuotation(customer)) {
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
            when (val result = quotesRepository.getPaymentTermsForQuotation(accountId)) {
                is ApiResult.Success -> {
                    val terms = result.data
                    val preferred = _uiState.value.selectedPaymentTerm
                    val selected = when {
                        preferred == null -> terms.firstOrNull()
                        preferred.id.isNotBlank() ->
                            terms.firstOrNull { it.id == preferred.id } ?: preferred
                        preferred.name.isNotBlank() ->
                            terms.firstOrNull {
                                it.name.equals(preferred.name, ignoreCase = true)
                            } ?: preferred
                        else -> terms.firstOrNull()
                    }
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = terms,
                        selectedPaymentTerm = selected,
                        isPaymentTermsLoading = false,
                        fieldErrors = _uiState.value.fieldErrors.withPaymentTermValid(selected != null)
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

    private fun validateDraft(): AddSaleOrderFieldErrors {
        val state = _uiState.value
        return AddSaleOrderFieldErrors(
            customer = state.selectedCustomer == null,
            billingAddress = state.selectedBillingAddress == null,
            shippingAddress = state.selectedShippingAddress == null,
            paymentTerm = state.selectedPaymentTerm == null,
            deliveryDate = state.deliveryDate.isBlank(),
            shippingMethod = state.selectedShippingMethod == null,
            products = state.selectedProducts.isEmpty()
        )
    }

    private fun loadSaleOrderDraft(saleOrderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                editSaleOrderId = saleOrderId,
                isEditMode = true,
                isEditLoading = true,
                isEditLoadFailed = false,
                errorMessage = null
            )
            when (val result = saleOrdersRepository.getSaleOrderForEdit(saleOrderId)) {
                is ApiResult.Success -> {
                    val draft = enrichDraftFromCustomerDetails(result.data)
                    val shippingMethod = _uiState.value.shippingMethods
                        .firstOrNull { it.id == draft.shippingMethodId }
                        ?: _uiState.value.shippingMethods.firstOrNull()
                    val products = draft.products.map { product ->
                        QuoteProductScheduleHelper.ensureSchedules(
                            product = product,
                            quoteShippingAddress = draft.shippingAddress,
                            quoteDeliveryDate = draft.deliveryDate
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        editSaleOrderId = draft.saleOrderId,
                        editOrderNumber = draft.orderNumber,
                        editStatus = draft.status,
                        isEditMode = true,
                        isEditLoading = false,
                        isEditLoadFailed = false,
                        selectedCustomer = draft.customer,
                        selectedBillingAddress = draft.billingAddress,
                        selectedShippingAddress = draft.shippingAddress,
                        selectedPaymentTerm = draft.paymentTerm,
                        paymentTerms = listOfNotNull(draft.paymentTerm.takeIf { it.name.isNotBlank() }),
                        deliveryDate = draft.deliveryDate,
                        selectedShippingMethod = shippingMethod,
                        selectedProducts = products,
                        fieldErrors = AddSaleOrderFieldErrors()
                    ).withCleanEditSnapshot()
                    if (draft.customer.id.isNotBlank()) {
                        loadPaymentTerms(draft.customer.id)
                    }
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
                        errorMessage = "Sale order details not found."
                    )
                }
            }
        }
    }

    private suspend fun enrichDraftFromCustomerDetails(
        draft: EditSaleOrderDraftUi
    ): EditSaleOrderDraftUi {
        val resolvedCustomer = resolveCustomerForEdit(draft.customer) ?: return draft
        return when (val result = quotesRepository.getCustomerDetailsForQuotation(resolvedCustomer)) {
            is ApiResult.Success -> {
                val enrichedCustomer = result.data.copy(
                    priceProfileId = resolvedCustomer.priceProfileId.ifBlank { result.data.priceProfileId },
                    priceProfile = resolvedCustomer.priceProfile.ifBlank { result.data.priceProfile },
                    creditHoldEnabled = resolvedCustomer.creditHoldEnabled ?: result.data.creditHoldEnabled,
                    remainingCredit = resolvedCustomer.remainingCredit.ifBlank { result.data.remainingCredit },
                    email = resolvedCustomer.email.ifBlank { result.data.email },
                    accountExecutive = resolvedCustomer.accountExecutive.ifBlank {
                        result.data.accountExecutive
                    }
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
            else -> draft.copy(customer = resolvedCustomer)
        }
    }

    private suspend fun resolveCustomerForEdit(customer: QuoteCustomerUi): QuoteCustomerUi? {
        if (customer.id.isNotBlank()) return customer
        val name = customer.name.trim()
        if (name.isBlank()) return customer
        return when (val result = quotesRepository.searchCustomers(name)) {
            is ApiResult.Success -> {
                val exact = result.data.firstOrNull {
                    it.name.equals(name, ignoreCase = true)
                }
                exact ?: result.data.firstOrNull() ?: customer
            }
            else -> customer
        }
    }

    private fun resolveAddress(
        preferred: QuoteAddressUi,
        options: List<QuoteAddressUi>
    ): QuoteAddressUi? {
        val matchedById = options.find { it.id == preferred.id && preferred.id.isNotBlank() }
        if (matchedById != null) return matchedById
        val preferredText = preferred.displayText.lowercase(Locale.US)
        if (preferredText.isNotBlank()) {
            val matchedByText = options.find { option ->
                option.displayText.lowercase(Locale.US).contains(preferredText) ||
                    preferredText.contains(option.displayText.lowercase(Locale.US)) ||
                    option.address.equals(preferred.address, ignoreCase = true)
            }
            if (matchedByText != null) return matchedByText
        }
        if (preferred.address.isNotBlank()) return preferred
        return options.firstOrNull()
    }

    private fun buildSaveRequest(): SaveSaleOrderRequest? {
        val state = _uiState.value
        val customer = state.selectedCustomer ?: return null
        val billingAddress = state.selectedBillingAddress ?: return null
        val shippingAddress = state.selectedShippingAddress ?: return null
        val paymentTerm = state.selectedPaymentTerm ?: return null
        val shippingMethod = state.selectedShippingMethod ?: return null
        return SaveSaleOrderRequest(
            saleOrderId = state.editSaleOrderId,
            orderNumber = state.editOrderNumber,
            customer = customer,
            billingAddress = billingAddress,
            shippingAddress = shippingAddress,
            paymentTerm = paymentTerm,
            deliveryDate = state.deliveryDate,
            shippingType = shippingMethod.id.toIntOrNull() ?: 1,
            products = state.selectedProducts
        )
    }

    private fun AddSaleOrderUiState.withDirtyState(): AddSaleOrderUiState {
        return if (isEditMode) {
            copy(hasUnsavedChanges = fingerprint() != originalEditFingerprint)
        } else {
            copy(hasUnsavedChanges = hasAnyDraftInput())
        }
    }

    private fun AddSaleOrderUiState.withCleanEditSnapshot(): AddSaleOrderUiState {
        if (!isEditMode) return this
        originalEditFingerprint = fingerprint()
        return copy(hasUnsavedChanges = false)
    }

    private fun AddSaleOrderUiState.fingerprint(): String {
        return listOf(
            selectedCustomer?.id.orEmpty(),
            selectedBillingAddress?.id.orEmpty(),
            selectedShippingAddress?.id.orEmpty(),
            selectedPaymentTerm?.id.orEmpty(),
            deliveryDate,
            selectedShippingMethod?.id.orEmpty(),
            selectedProducts.joinToString("|") { product ->
                listOf(
                    product.id,
                    product.lineItemId.orEmpty(),
                    product.quantity.toString(),
                    product.unitPrice.toString(),
                    product.deliverySchedules.joinToString(",") { schedule ->
                        "${schedule.shippingAddress.id}:${schedule.quantity}:${schedule.estimatedDeliveryDate}"
                    }
                ).joinToString(":")
            }
        ).joinToString("#")
    }

    private fun AddSaleOrderUiState.hasAnyDraftInput(): Boolean {
        return selectedCustomer != null ||
            selectedBillingAddress != null ||
            selectedShippingAddress != null ||
            selectedPaymentTerm != null ||
            deliveryDate.isNotBlank() ||
            selectedProducts.isNotEmpty()
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        const val ARG_SALE_ORDER_ID = "saleOrderId"
        const val RESULT_SALE_ORDER_CREATED = "saleOrderCreated"
    }
}

data class SaleOrderShippingMethod(
    val id: String,
    @StringRes val nameResId: Int
) {
    companion object {
        fun defaults(): List<SaleOrderShippingMethod> = listOf(
            SaleOrderShippingMethod("1", R.string.add_sale_order_shipping_method_ground),
            SaleOrderShippingMethod("2", R.string.add_sale_order_shipping_method_express),
            SaleOrderShippingMethod("3", R.string.add_sale_order_shipping_method_overnight),
            SaleOrderShippingMethod("4", R.string.add_sale_order_shipping_method_pickup)
        )
    }
}

data class AddSaleOrderFieldErrors(
    val customer: Boolean = false,
    val billingAddress: Boolean = false,
    val shippingAddress: Boolean = false,
    val paymentTerm: Boolean = false,
    val deliveryDate: Boolean = false,
    val shippingMethod: Boolean = false,
    val products: Boolean = false
) {
    fun hasAny(): Boolean {
        return customer || billingAddress || shippingAddress || paymentTerm ||
            deliveryDate || shippingMethod || products
    }

    fun withCustomerValid(isValid: Boolean) = if (isValid) copy(customer = false) else this
    fun withBillingAddressValid(isValid: Boolean) = if (isValid) copy(billingAddress = false) else this
    fun withShippingAddressValid(isValid: Boolean) = if (isValid) copy(shippingAddress = false) else this
    fun withPaymentTermValid(isValid: Boolean) = if (isValid) copy(paymentTerm = false) else this
    fun withDeliveryDateValid(isValid: Boolean) = if (isValid) copy(deliveryDate = false) else this
    fun withShippingMethodValid(isValid: Boolean) = if (isValid) copy(shippingMethod = false) else this
    fun withProductsValid(isValid: Boolean) = if (isValid) copy(products = false) else this
}

data class AddSaleOrderUiState(
    val editSaleOrderId: String? = null,
    val isEditMode: Boolean = false,
    val isEditLoading: Boolean = false,
    val isEditLoadFailed: Boolean = false,
    val customerSearchQuery: String = "",
    val customers: List<QuoteCustomerUi> = emptyList(),
    val paymentTerms: List<QuotePaymentTermUi> = emptyList(),
    val shippingMethods: List<SaleOrderShippingMethod> = emptyList(),
    val editOrderNumber: String = "",
    val editStatus: SaleOrderStatus? = null,
    val hasUnsavedChanges: Boolean = false,
    val selectedCustomer: QuoteCustomerUi? = null,
    val selectedBillingAddress: QuoteAddressUi? = null,
    val selectedShippingAddress: QuoteAddressUi? = null,
    val selectedPaymentTerm: QuotePaymentTermUi? = null,
    val selectedShippingMethod: SaleOrderShippingMethod? = null,
    val deliveryDate: String = "",
    val selectedProducts: List<QuoteCreationProductUi> = emptyList(),
    val isCustomerSearchLoading: Boolean = false,
    val isCustomerDetailsLoading: Boolean = false,
    val isPaymentTermsLoading: Boolean = false,
    val errorMessage: String? = null,
    val fieldErrors: AddSaleOrderFieldErrors = AddSaleOrderFieldErrors(),
    val scrollToFirstValidationError: Boolean = false
) {
    val isFormLoading: Boolean
        get() = isCustomerDetailsLoading || isPaymentTermsLoading || isEditLoading

    val canSave: Boolean
        get() = if (isEditMode) {
            (editStatus == SaleOrderStatus.DRAFT || editStatus == SaleOrderStatus.PENDING) &&
                hasUnsavedChanges
        } else {
            hasUnsavedChanges
        }

    val subtotal: Double
        get() = selectedProducts.sumOf { it.lineTotal }

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
    }
}
