package com.axelliant.hris.features.purchaseorders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoProductLineUi
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoSaleOrderInfoUi
import com.axelliant.hris.features.purchaseorders.domain.model.CreatePurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.domain.model.SaleOrderDdlUi
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationRequest
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.axelliant.hris.features.saleorders.data.SaleOrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.round
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AddPurchaseOrderUiState(
    val quoteSearchQuery: String = "",
    val quotes: List<QuoteModel> = emptyList(),
    val isQuoteSearchLoading: Boolean = false,
    val selectedQuote: QuoteModel? = null,
    val saleOrderSearchQuery: String = "",
    val saleOrders: List<SaleOrderDdlUi> = emptyList(),
    val isSaleOrderSearchLoading: Boolean = false,
    val selectedSaleOrder: SaleOrderDdlUi? = null,
    val saleOrderInfo: AddPoSaleOrderInfoUi? = null,
    val isSaleOrderInfoExpanded: Boolean = true,
    val isSaleOrderInfoLoading: Boolean = false,
    val fieldErrors: AddPurchaseOrderFieldErrors = AddPurchaseOrderFieldErrors(),
    val errorMessage: String? = null
) {
    val selectedProducts: List<AddPoProductLineUi>
        get() = saleOrderInfo?.products.orEmpty().filter { it.isSelected }

    val selectedItemCount: Int
        get() = selectedProducts.size

    val totalSelectedQty: Int
        get() = selectedProducts.sumOf { it.quantity }

    val subtotal: Double
        get() = selectedProducts.sumOf { it.lineTotal }

    val tax: Double
        get() = saleOrderInfo?.tax ?: 0.0

    val shipping: Double
        get() = saleOrderInfo?.shipping ?: 0.0

    val grandTotal: Double
        get() = subtotal + tax + shipping

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(round(amount * 100) / 100.0)
    }
}

data class AddPurchaseOrderFieldErrors(
    val quote: Boolean = false,
    val saleOrder: Boolean = false,
    val products: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddPurchaseOrderViewModel @Inject constructor(
    private val quotesRepository: QuotesRepository,
    private val saleOrdersRepository: SaleOrdersRepository,
    private val purchaseOrdersRepository: PurchaseOrdersRepository
) : ViewModel() {

    private val quoteSearchInput = MutableStateFlow("")
    private val saleOrderSearchInput = MutableStateFlow("")

    private var saleOrderInfoJob: Job? = null

    private val _uiState = MutableStateFlow(AddPurchaseOrderUiState())
    val uiState = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val createState = _createState.asStateFlow()

    init {
        observeQuoteSearch()
        observeSaleOrderSearch()
    }

    private fun observeQuoteSearch() {
        quoteSearchInput
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach(::searchQuotes)
            .launchIn(viewModelScope)
    }

    private fun observeSaleOrderSearch() {
        saleOrderSearchInput
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach(::searchSaleOrders)
            .launchIn(viewModelScope)
    }

    fun onQuoteSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(quoteSearchQuery = query)
        quoteSearchInput.value = query
    }

    fun onSaleOrderSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(saleOrderSearchQuery = query)
        saleOrderSearchInput.value = query
    }

    fun clearSelectedQuote() {
        saleOrderInfoJob?.cancel()
        _uiState.value = AddPurchaseOrderUiState()
        quoteSearchInput.value = ""
        saleOrderSearchInput.value = ""
    }

    fun selectQuote(quote: QuoteModel) {
        saleOrderInfoJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedQuote = quote,
            selectedSaleOrder = null,
            saleOrderInfo = null,
            saleOrders = emptyList(),
            saleOrderSearchQuery = "",
            isSaleOrderSearchLoading = true,
            fieldErrors = _uiState.value.fieldErrors.copy(quote = false, saleOrder = false, products = false),
            errorMessage = null
        )
        saleOrderSearchInput.value = ""
        viewModelScope.launch {
            searchSaleOrders("")
        }
    }

    fun selectSaleOrder(saleOrder: SaleOrderDdlUi) {
        val quote = _uiState.value.selectedQuote ?: return
        saleOrderInfoJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedSaleOrder = saleOrder,
            isSaleOrderInfoLoading = true,
            saleOrderInfo = null,
            fieldErrors = _uiState.value.fieldErrors.copy(saleOrder = false, products = false),
            errorMessage = null
        )
        saleOrderInfoJob = viewModelScope.launch {
            val previewResult = quotesRepository.getQuotePreview(quote.id)
            val preview = (previewResult as? ApiResult.Success)?.data
            when (
                val result = saleOrdersRepository.getSaleOrderPosInfo(
                    orderId = saleOrder.id,
                    quotationId = quote.id,
                    quotationNumber = quote.quoteId,
                    quotePreview = preview
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        saleOrderInfo = result.data.copy(
                            quoteNumber = result.data.quoteNumber.ifBlank { quote.quoteId }
                        ),
                        isSaleOrderInfoLoading = false,
                        errorMessage = null
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isSaleOrderInfoLoading = false,
                        errorMessage = resolveError(result, "Unable to load sales order information.")
                    )
                }
            }
        }
    }

    fun toggleSaleOrderInfoExpanded() {
        _uiState.value = _uiState.value.copy(
            isSaleOrderInfoExpanded = !_uiState.value.isSaleOrderInfoExpanded
        )
    }

    fun toggleProductSelected(productId: String) {
        updateProducts { products ->
            products.map { product ->
                if (product.id == productId) product.copy(isSelected = !product.isSelected) else product
            }
        }
    }

    fun updateProductQuantity(productId: String, quantityText: String) {
        val quantity = quantityText.filter { it.isDigit() }.toIntOrNull() ?: 0
        updateProducts { products ->
            products.map { product ->
                if (product.id != productId) return@map product
                product.copy(quantity = quantity.coerceIn(0, product.maxQuantity))
            }
        }
    }

    fun selectProductVendor(productId: String, vendor: String) {
        updateProducts { products ->
            products.map { product ->
                if (product.id == productId) product.copy(vendor = vendor) else product
            }
        }
    }

    fun clearErrorMessage() {
        if (_uiState.value.errorMessage != null) {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    fun clearCreateState() {
        _createState.value = UiState.Idle
    }

    fun createPurchaseOrder() {
        val state = _uiState.value
        val quote = state.selectedQuote
        val saleOrder = state.selectedSaleOrder
        val info = state.saleOrderInfo
        val quoteMissing = quote == null
        val saleOrderMissing = saleOrder == null || info == null
        val productsMissing = state.selectedProducts.isEmpty()
        if (quoteMissing || saleOrderMissing || productsMissing) {
            _uiState.value = state.copy(
                fieldErrors = AddPurchaseOrderFieldErrors(
                    quote = quoteMissing,
                    saleOrder = saleOrderMissing,
                    products = productsMissing
                )
            )
            return
        }
        val selectedQuote = quote ?: return
        val selectedSaleOrder = saleOrder ?: return
        val selectedSaleOrderInfo = info ?: return

        _createState.value = UiState.Loading
        viewModelScope.launch {
            val request = CreatePurchaseOrderRequest(
                quotationId = selectedQuote.id,
                quotationNumber = selectedQuote.quoteId,
                saleOrderId = selectedSaleOrder.id,
                saleOrderNumber = selectedSaleOrder.orderNumber,
                customerName = selectedSaleOrderInfo.customerName,
                products = selectedSaleOrderInfo.products,
                subtotal = state.subtotal,
                tax = state.tax,
                shipping = state.shipping,
                grandTotal = state.grandTotal,
                saleOrderInfo = selectedSaleOrderInfo
            )
            when (val result = purchaseOrdersRepository.createPurchaseOrder(request)) {
                is ApiResult.Success -> {
                    _createState.value = UiState.Success(result.data.message)
                }
                else -> {
                    _createState.value = UiState.Error(
                        resolveError(result, "Unable to create purchase order.")
                    )
                }
            }
        }
    }

    private fun updateProducts(transform: (List<AddPoProductLineUi>) -> List<AddPoProductLineUi>) {
        val info = _uiState.value.saleOrderInfo ?: return
        val updated = info.copy(products = transform(info.products))
        _uiState.value = _uiState.value.copy(
            saleOrderInfo = updated,
            fieldErrors = _uiState.value.fieldErrors.copy(products = false)
        )
    }

    private suspend fun searchQuotes(query: String) {
        _uiState.value = _uiState.value.copy(isQuoteSearchLoading = true)
        val request = GetQuotationRequest(
            start = 0,
            limit = QuotesRepository.PAGE_SIZE,
            search = query.trim(),
            quoteType = QuotesRepository.QUOTE_TYPE_STANDARD,
            approvalStatus = QuoteStatus.Approved.apiValue,
            isPaginated = true
        )
        when (val result = quotesRepository.getAllQuotes(request)) {
            is ApiResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    quotes = result.data.quotes,
                    isQuoteSearchLoading = false
                )
            }
            else -> {
                // Fallback to unfiltered list so POS still works if Approved filter returns empty.
                when (
                    val fallback = quotesRepository.getAllQuotes(
                        request.copy(approvalStatus = null)
                    )
                ) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            quotes = fallback.data.quotes,
                            isQuoteSearchLoading = false
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            quotes = emptyList(),
                            isQuoteSearchLoading = false,
                            errorMessage = resolveError(result, "Unable to load quotes.")
                        )
                    }
                }
            }
        }
    }

    private suspend fun searchSaleOrders(query: String) {
        val quote = _uiState.value.selectedQuote
        if (quote == null) {
            _uiState.value = _uiState.value.copy(
                saleOrders = emptyList(),
                isSaleOrderSearchLoading = false
            )
            return
        }
        _uiState.value = _uiState.value.copy(isSaleOrderSearchLoading = true)
        when (
            val result = saleOrdersRepository.getSaleOrdersForQuote(
                quotationId = quote.id,
                quotationNumber = quote.quoteId,
                customerName = quote.customerName,
                search = query
            )
        ) {
            is ApiResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    saleOrders = result.data,
                    isSaleOrderSearchLoading = false
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    saleOrders = emptyList(),
                    isSaleOrderSearchLoading = false,
                    errorMessage = resolveError(result, "Unable to load sales orders.")
                )
            }
        }
    }

    private fun resolveError(result: ApiResult<*>, fallback: String): String {
        return when (result) {
            is ApiResult.HttpError -> result.message?.takeIf { it.isNotBlank() } ?: fallback
            is ApiResult.NetworkError -> result.message?.takeIf { it.isNotBlank() } ?: fallback
            is ApiResult.UnknownError -> result.message?.takeIf { it.isNotBlank() } ?: fallback
            ApiResult.Unauthorized -> "Session expired. Please sign in again."
            else -> fallback
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
