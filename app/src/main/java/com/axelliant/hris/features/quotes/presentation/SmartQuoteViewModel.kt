package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.data.SmartQuoteProductSearchHistory
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import dagger.hilt.android.lifecycle.HiltViewModel
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
class SmartQuoteViewModel @Inject constructor(
    private val repository: QuotesRepository,
    private val productSearchHistory: SmartQuoteProductSearchHistory,
    private val suggestionStore: SmartQuoteSuggestionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartQuoteUiState())
    val uiState = _uiState.asStateFlow()
    private val customerSearchInput = MutableStateFlow("")
    private var customerSearchJob: Job? = null
    private var customerDetailsJob: Job? = null
    private var paymentTermsJob: Job? = null

    init {
        loadSuggestions()
        refreshFrequentSearches()
        observeCustomerSearch()
    }

    fun refreshFrequentSearches() {
        _uiState.value = _uiState.value.copy(
            frequentSearchQueries = productSearchHistory.getFrequent()
        )
    }

    fun applySelectedProducts(products: List<QuoteCreationProductUi>, searchQuery: String?) {
        val trimmedQuery = searchQuery?.trim().orEmpty()
        if (trimmedQuery.isNotBlank()) {
            productSearchHistory.record(trimmedQuery)
        }
        val current = _uiState.value
        val existingById = current.selectedProducts.associateBy { it.id }
        val merged = products.map { incoming ->
            existingById[incoming.id]?.copy(
                name = incoming.name,
                sku = incoming.sku,
                category = incoming.category,
                thumbnailLabel = incoming.thumbnailLabel,
                brandThumbnail = incoming.brandThumbnail,
                unitPrice = incoming.unitPrice
            ) ?: incoming
        }
        _uiState.value = current.copy(
            selectedProducts = merged,
            frequentSearchQueries = productSearchHistory.getFrequent(),
            errorMessage = null
        )
    }

    fun updateSelectedProductQuantity(productId: String, delta: Int) {
        val current = _uiState.value
        if (delta < 0) {
            val product = current.selectedProducts.firstOrNull { it.id == productId } ?: return
            if (product.quantity <= 1) {
                removeSelectedProduct(productId)
                return
            }
        }
        _uiState.value = current.copy(
            selectedProducts = current.selectedProducts.map { product ->
                if (product.id != productId) {
                    product
                } else {
                    product.copy(quantity = (product.quantity + delta).coerceAtLeast(1))
                }
            }
        )
    }

    fun removeSelectedProduct(productId: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedProducts = current.selectedProducts.filterNot { it.id == productId }
        )
    }

    fun updateSelectedProduct(product: QuoteCreationProductUi) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedProducts = current.selectedProducts.map { existing ->
                if (existing.id == product.id) product else existing
            }
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomerSearch() {
        customerSearchInput
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach(::searchCustomers)
            .launchIn(viewModelScope)
    }

    fun onInputChanged(input: String) {
        val current = _uiState.value
        _uiState.value = if (current.step == SmartQuoteStep.DeliveryDate) {
            current.copy(
                input = input,
                deliveryDate = input,
                errorMessage = null
            )
        } else {
            current.copy(
                input = input,
                errorMessage = null
            )
        }
        if (current.step == SmartQuoteStep.Customer) {
            customerSearchInput.value = input
        }
    }

    fun onOptionSelected(option: String) {
        val current = _uiState.value
        _uiState.value = when (current.step) {
            SmartQuoteStep.QuoteTitle -> {
                suggestionStore.rememberQuoteTitle(option)
                current.copy(
                    quoteTitle = option,
                    input = option,
                    recentQuoteTitles = suggestionStore.quoteTitles(),
                    errorMessage = null
                )
            }
            SmartQuoteStep.Customer -> current.copy(
                input = option,
                errorMessage = null
            ).let {
                val customer = current.customerOptions().firstOrNull { customer ->
                    customer.name.equals(option, ignoreCase = true)
                }
                if (customer == null) {
                    it
                } else {
                    selectCustomer(customer)
                    _uiState.value
                }
            }
            SmartQuoteStep.LineItems -> current
            SmartQuoteStep.ShippingAddress -> selectShippingAddress(option, current)
            SmartQuoteStep.BillingAddress -> selectBillingAddress(option, current)
            SmartQuoteStep.PaymentTerms -> selectPaymentTerm(option, current)
            else -> current.copy(input = option, errorMessage = null)
        }
    }

    fun onDateSelected(date: String) {
        _uiState.value = _uiState.value.copy(
            deliveryDate = date,
            input = date,
            errorMessage = null
        )
    }

    fun onVoiceAnswer(answer: String) {
        val normalized = answer.trim()
        if (normalized.isBlank()) return
        val current = _uiState.value
        if (current.step == SmartQuoteStep.LineItems) return
        val matchedOption = current.visibleOptions().firstOrNull { option ->
            option.contains(normalized, ignoreCase = true) ||
                normalized.contains(option, ignoreCase = true)
        }
        if (matchedOption != null && current.step.usesOptions) {
            onOptionSelected(matchedOption)
        } else {
            onInputChanged(normalized)
            if (current.step == SmartQuoteStep.QuoteTitle) {
                suggestionStore.rememberQuoteTitle(normalized)
                _uiState.value = _uiState.value.copy(
                    quoteTitle = normalized,
                    recentQuoteTitles = suggestionStore.quoteTitles()
                )
            }
            if (current.step == SmartQuoteStep.DeliveryDate) {
                _uiState.value = _uiState.value.copy(deliveryDate = normalized)
            }
        }
    }

    fun onNext(): Boolean {
        val current = persistCurrentInput(_uiState.value)
        val nextStep = current.step.next()
        if (current.step == SmartQuoteStep.Review) {
            val missing = current.missingFields()
            if (missing.isNotEmpty()) {
                _uiState.value = current.copy(
                    missingFields = missing,
                    errorMessage = "Complete missing fields before saving."
                )
                return false
            }
            _uiState.value = current.copy(isFinished = true, missingFields = emptyList())
            return true
        }

        _uiState.value = current.copy(
            step = nextStep ?: SmartQuoteStep.Review,
            input = current.inputFor(nextStep ?: SmartQuoteStep.Review),
            errorMessage = null,
            missingFields = emptyList()
        )
        return true
    }

    fun onBack(): Boolean {
        val current = persistCurrentInput(_uiState.value)
        val previous = current.step.previous() ?: return false
        _uiState.value = current.copy(
            step = previous,
            input = current.inputFor(previous),
            errorMessage = null,
            missingFields = emptyList()
        )
        return true
    }

    fun navigateToStep(step: SmartQuoteStep) {
        val current = persistCurrentInput(_uiState.value)
        _uiState.value = current.copy(
            step = step,
            input = current.inputFor(step),
            errorMessage = null
        )
    }

    private fun persistCurrentInput(state: SmartQuoteUiState): SmartQuoteUiState {
        val trimmed = state.input.trim()
        return when (state.step) {
            SmartQuoteStep.QuoteTitle -> {
                suggestionStore.rememberQuoteTitle(trimmed)
                state.copy(
                    quoteTitle = trimmed,
                    recentQuoteTitles = suggestionStore.quoteTitles()
                )
            }
            SmartQuoteStep.DeliveryDate -> state.copy(deliveryDate = trimmed)
            else -> state
        }
    }

    private fun selectCustomer(customer: QuoteCustomerUi) {
        customerDetailsJob?.cancel()
        paymentTermsJob?.cancel()
        suggestionStore.rememberCustomer(customer)
        val recentCustomers = suggestionStore.customers()
        _uiState.value = _uiState.value.copy(
            selectedCustomer = customer,
            customer = customer.name,
            input = customer.name,
            recentCustomers = recentCustomers,
            selectedBillingAddress = null,
            selectedShippingAddress = null,
            selectedPaymentTerm = null,
            billingAddress = "",
            shippingAddress = "",
            paymentTerm = "",
            paymentTerms = emptyList(),
            isCustomerDetailsLoading = true,
            isPaymentTermsLoading = false,
            errorMessage = null
        )
        loadCustomerDetails(customer)
    }

    private fun selectShippingAddress(option: String, current: SmartQuoteUiState): SmartQuoteUiState {
        val address = current.selectedCustomer?.shippingAddresses
            .orEmpty()
            .firstOrNull { it.optionLabel() == option }
        return current.copy(
            selectedShippingAddress = address,
            shippingAddress = address?.displayTextWithLocation ?: option,
            input = option,
            errorMessage = null
        )
    }

    private fun selectBillingAddress(option: String, current: SmartQuoteUiState): SmartQuoteUiState {
        if (option == SAME_AS_SHIPPING) {
            return current.copy(
                selectedBillingAddress = current.selectedShippingAddress,
                billingAddress = current.shippingAddress,
                input = option,
                errorMessage = null
            )
        }
        val address = current.selectedCustomer?.billingAddresses
            .orEmpty()
            .firstOrNull { it.optionLabel() == option }
        return current.copy(
            selectedBillingAddress = address,
            billingAddress = address?.displayTextWithLocation ?: option,
            input = option,
            errorMessage = null
        )
    }

    private fun selectPaymentTerm(option: String, current: SmartQuoteUiState): SmartQuoteUiState {
        val term = current.paymentTerms.firstOrNull { it.optionLabel() == option }
        return current.copy(
            selectedPaymentTerm = term,
            paymentTerm = term?.name ?: option,
            input = option,
            errorMessage = null
        )
    }

    private fun searchCustomers(query: String) {
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCustomerSearchLoading = true,
                errorMessage = null
            )
            when (val result = repository.searchCustomers(query)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    customers = result.data,
                    isCustomerSearchLoading = false
                )
                is ApiResult.HttpError -> setSearchError(result.message)
                is ApiResult.NetworkError -> setSearchError(result.message)
                is ApiResult.UnknownError -> setSearchError(result.message)
                ApiResult.Empty -> _uiState.value = _uiState.value.copy(
                    customers = emptyList(),
                    isCustomerSearchLoading = false
                )
                ApiResult.Unauthorized -> setSearchError("Session expired. Please login again.")
            }
        }
    }

    private fun loadCustomerDetails(customer: QuoteCustomerUi) {
        customerDetailsJob = viewModelScope.launch {
            when (val result = repository.getCustomerDetailsForQuotation(customer)) {
                is ApiResult.Success -> {
                    if (_uiState.value.selectedCustomer?.id != customer.id) return@launch
                    val enriched = result.data
                    val billing = enriched.billingAddresses.firstOrNull()
                    val shipping = enriched.shippingAddresses.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        selectedCustomer = enriched,
                        selectedBillingAddress = billing,
                        selectedShippingAddress = shipping,
                        billingAddress = billing?.displayTextWithLocation.orEmpty(),
                        shippingAddress = shipping?.displayTextWithLocation.orEmpty(),
                        isCustomerDetailsLoading = false,
                        errorMessage = null
                    )
                    loadPaymentTerms(enriched.id)
                }
                is ApiResult.HttpError -> setCustomerDetailsError(customer.id, result.message)
                is ApiResult.NetworkError -> setCustomerDetailsError(customer.id, result.message)
                is ApiResult.UnknownError -> setCustomerDetailsError(customer.id, result.message)
                ApiResult.Empty -> setCustomerDetailsError(customer.id, "Customer details not found.")
                ApiResult.Unauthorized -> setCustomerDetailsError(customer.id, "Session expired. Please login again.")
            }
        }
    }

    private fun loadPaymentTerms(accountId: String) {
        paymentTermsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPaymentTermsLoading = true)
            when (val result = repository.getPaymentTermsForQuotation(accountId)) {
                is ApiResult.Success -> {
                    val defaultTerm = result.data.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        paymentTerms = result.data,
                        selectedPaymentTerm = defaultTerm,
                        paymentTerm = defaultTerm?.name.orEmpty(),
                        isPaymentTermsLoading = false,
                        errorMessage = null
                    )
                }
                is ApiResult.HttpError -> setPaymentTermsError(result.message)
                is ApiResult.NetworkError -> setPaymentTermsError(result.message)
                is ApiResult.UnknownError -> setPaymentTermsError(result.message)
                ApiResult.Empty -> _uiState.value = _uiState.value.copy(
                    paymentTerms = emptyList(),
                    isPaymentTermsLoading = false
                )
                ApiResult.Unauthorized -> setPaymentTermsError("Session expired. Please login again.")
            }
        }
    }

    private fun setSearchError(message: String) {
        _uiState.value = _uiState.value.copy(
            customers = emptyList(),
            isCustomerSearchLoading = false,
            errorMessage = message
        )
    }

    private fun setCustomerDetailsError(customerId: String, message: String) {
        if (_uiState.value.selectedCustomer?.id != customerId) return
        _uiState.value = _uiState.value.copy(
            isCustomerDetailsLoading = false,
            errorMessage = message
        )
    }

    private fun setPaymentTermsError(message: String) {
        _uiState.value = _uiState.value.copy(
            paymentTerms = emptyList(),
            isPaymentTermsLoading = false,
            errorMessage = message
        )
    }

    private fun loadSuggestions() {
        _uiState.value = _uiState.value.copy(
            recentQuoteTitles = suggestionStore.quoteTitles(),
            recentCustomers = suggestionStore.customers(),
            recentProducts = suggestionStore.products()
        )
    }

    companion object {
        const val SAME_AS_SHIPPING = "Same as Shipping"
        const val RESULT_PRODUCTS = "smartQuoteProducts"
        const val RESULT_SEARCH_QUERY = "smartQuoteProductSearchQuery"
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}

data class SmartQuoteUiState(
    val step: SmartQuoteStep = SmartQuoteStep.QuoteTitle,
    val input: String = "",
    val quoteTitle: String = "",
    val customer: String = "",
    val customers: List<QuoteCustomerUi> = emptyList(),
    val recentQuoteTitles: List<String> = emptyList(),
    val recentCustomers: List<QuoteCustomerUi> = emptyList(),
    val recentProducts: List<String> = emptyList(),
    val selectedCustomer: QuoteCustomerUi? = null,
    val selectedProducts: List<QuoteCreationProductUi> = emptyList(),
    val frequentSearchQueries: List<String> = emptyList(),
    val shippingAddress: String = "",
    val selectedShippingAddress: QuoteAddressUi? = null,
    val billingAddress: String = "",
    val selectedBillingAddress: QuoteAddressUi? = null,
    val deliveryDate: String = "",
    val paymentTerm: String = "",
    val paymentTerms: List<QuotePaymentTermUi> = emptyList(),
    val selectedPaymentTerm: QuotePaymentTermUi? = null,
    val errorMessage: String? = null,
    val missingFields: List<SmartQuoteMissingField> = emptyList(),
    val isCustomerSearchLoading: Boolean = false,
    val isCustomerDetailsLoading: Boolean = false,
    val isPaymentTermsLoading: Boolean = false,
    val isFinished: Boolean = false
) {
    val progressText: String
        get() = if (step == SmartQuoteStep.Review) "100%" else "${step.position} of $INPUT_STEP_COUNT"

    val progressPercent: Int
        get() = if (step == SmartQuoteStep.Review) {
            100
        } else {
            ((step.position.toFloat() / INPUT_STEP_COUNT) * 100).toInt()
        }

    val canGoBack: Boolean
        get() = step.previous() != null

    val nextButtonText: String
        get() = if (step == SmartQuoteStep.Review) "Finalize Quote" else "Next"

    fun visibleOptions(): List<String> {
        val source = when (step) {
            SmartQuoteStep.QuoteTitle -> quoteTitleSuggestions()
            SmartQuoteStep.Customer -> customerOptions().map { it.name }
            SmartQuoteStep.LineItems -> emptyList()
            SmartQuoteStep.ShippingAddress -> selectedCustomer?.shippingAddresses.orEmpty()
                .map { it.optionLabel() }
            SmartQuoteStep.BillingAddress -> listOfNotNull(
                SmartQuoteViewModel.SAME_AS_SHIPPING.takeIf { shippingAddress.isNotBlank() }
            ) + selectedCustomer?.billingAddresses.orEmpty().map { it.optionLabel() }
            SmartQuoteStep.PaymentTerms -> paymentTerms.map { it.optionLabel() }
            else -> emptyList()
        }
        if (step != SmartQuoteStep.Customer) return source
        if (input.isBlank()) return source
        if (selectedCustomer?.name == input) return source
        return source.filter { it.contains(input, ignoreCase = true) }.ifEmpty { source }
    }

    fun customerOptions(): List<QuoteCustomerUi> {
        val recent = if (input.isBlank() || selectedCustomer?.name == input) {
            recentCustomers.take(MAX_VISIBLE_SUGGESTIONS)
        } else {
            recentCustomers
                .filter { it.name.contains(input, ignoreCase = true) }
                .take(MAX_VISIBLE_SUGGESTIONS)
        }
        return (recent + customers)
            .distinctBy { it.name.trim().lowercase() }
    }

    private fun quoteTitleSuggestions(): List<String> {
        val recent = recentQuoteTitles
            .filter { input.isBlank() || it.contains(input, ignoreCase = true) }
            .take(MAX_VISIBLE_SUGGESTIONS)
        if (recent.isNotEmpty()) return recent

        val typed = input.trim()
        if (typed.isNotBlank()) {
            return listOf(
                typed,
                "$typed Quote",
                "$typed Proposal",
                "$typed Renewal",
                "$typed Q${currentQuarter()} Quote"
            ).distinctBy { it.lowercase() }
                .take(MAX_VISIBLE_SUGGESTIONS)
        }

        return listOf(
            "Infrastructure Refresh",
            "Hardware Expansion",
            "Maintenance Renewal",
            "Customer Upgrade",
            "Q${currentQuarter()} Product Quote"
        )
    }

    fun inputFor(target: SmartQuoteStep): String {
        return when (target) {
            SmartQuoteStep.QuoteTitle -> quoteTitle
            SmartQuoteStep.Customer -> customer
            SmartQuoteStep.LineItems -> ""
            SmartQuoteStep.ShippingAddress -> shippingAddress
            SmartQuoteStep.BillingAddress -> billingAddress
            SmartQuoteStep.DeliveryDate -> deliveryDate
            SmartQuoteStep.PaymentTerms -> paymentTerm
            SmartQuoteStep.Review -> ""
        }
    }

    fun missingFields(): List<SmartQuoteMissingField> {
        return buildList {
            if (quoteTitle.isBlank()) add(SmartQuoteMissingField("Quote Title", SmartQuoteStep.QuoteTitle))
            if (customer.isBlank()) add(SmartQuoteMissingField("Customer", SmartQuoteStep.Customer))
            if (selectedProducts.isEmpty()) add(SmartQuoteMissingField("Products", SmartQuoteStep.LineItems))
            if (shippingAddress.isBlank()) add(SmartQuoteMissingField("Shipping Address", SmartQuoteStep.ShippingAddress))
            if (billingAddress.isBlank()) add(SmartQuoteMissingField("Billing Address", SmartQuoteStep.BillingAddress))
            if (deliveryDate.isBlank()) add(SmartQuoteMissingField("Delivery Date", SmartQuoteStep.DeliveryDate))
            if (paymentTerm.isBlank()) add(SmartQuoteMissingField("Payment Terms", SmartQuoteStep.PaymentTerms))
        }
    }

    companion object {
        const val INPUT_STEP_COUNT = 7
        const val MAX_VISIBLE_SUGGESTIONS = 5
    }
}

private fun QuoteAddressUi.optionLabel(): String = displayTextWithLocation.ifBlank { displayText }

private fun QuotePaymentTermUi.optionLabel(): String = description
    .takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) }
    ?.let { "$name\n$it" }
    ?: name

private fun currentQuarter(): Int {
    val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    return (month / 3) + 1
}

data class SmartQuoteMissingField(
    val label: String,
    val step: SmartQuoteStep
)

enum class SmartQuoteStep(
    val position: Int,
    val title: String,
    val nextLabel: String,
    val category: String,
    val question: String,
    val insight: String,
    val hint: String,
    val helper: String,
    val usesOptions: Boolean
) {
    QuoteTitle(
        position = 1,
        title = "Quote Title",
        nextLabel = "Customer",
        category = "",
        question = "What should we call this quote?",
        insight = "",
        hint = "e.g. Urban Tech Hub Q3",
        helper = "",
        usesOptions = false
    ),
    Customer(
        position = 2,
        title = "Customer",
        nextLabel = "Products",
        category = "",
        question = "Which customer is this quote for?",
        insight = "",
        hint = "Search accounts...",
        helper = "",
        usesOptions = true
    ),
    LineItems(
        position = 3,
        title = "Products",
        nextLabel = "Shipping Address",
        category = "",
        question = "Which products should we include?",
        insight = "",
        hint = "Search product name or SKU...",
        helper = "",
        usesOptions = true
    ),
    ShippingAddress(
        position = 4,
        title = "Shipping Address",
        nextLabel = "Billing Address",
        category = "",
        question = "Where should we ship the products?",
        insight = "",
        hint = "Search shipping address...",
        helper = "",
        usesOptions = true
    ),
    BillingAddress(
        position = 5,
        title = "Billing Address",
        nextLabel = "Delivery Date",
        category = "",
        question = "Which billing address should we use?",
        insight = "",
        hint = "Search billing address...",
        helper = "",
        usesOptions = true
    ),
    DeliveryDate(
        position = 6,
        title = "Delivery Date",
        nextLabel = "Payment Terms",
        category = "",
        question = "When should delivery arrive?",
        insight = "",
        hint = "MM-DD-YYYY",
        helper = "",
        usesOptions = false
    ),
    PaymentTerms(
        position = 7,
        title = "Payment Terms",
        nextLabel = "Review",
        category = "",
        question = "Which payment terms should apply?",
        insight = "",
        hint = "Select payment terms...",
        helper = "",
        usesOptions = true
    ),
    Review(
        position = 8,
        title = "Quote Preview",
        nextLabel = "Finalize Quote",
        category = "",
        question = "Review quote details",
        insight = "",
        hint = "",
        helper = "",
        usesOptions = false
    );

    fun next(): SmartQuoteStep? = entries.getOrNull(ordinal + 1)
    fun previous(): SmartQuoteStep? = entries.getOrNull(ordinal - 1)
}
