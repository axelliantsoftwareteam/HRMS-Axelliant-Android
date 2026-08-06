package com.axelliant.hris.features.purchaseorders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoProductLineUi
import com.axelliant.hris.features.purchaseorders.domain.model.CreatePurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.domain.model.ManualPoProductLineUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateManualPurchaseOrderUiState(
    val products: List<ManualPoProductLineUi> = emptyList(),
    val shipping: Double = 0.0,
    val tax: Double = 0.0,
    val showProductsValidation: Boolean = false
) {
    val hasProducts: Boolean
        get() = products.isNotEmpty()

    val subtotal: Double
        get() = products.sumOf { it.lineTotal }

    val grandTotal: Double
        get() = subtotal + shipping + tax

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(round(amount * 100) / 100.0)
    }
}

@HiltViewModel
class CreateManualPurchaseOrderViewModel @Inject constructor(
    private val purchaseOrdersRepository: PurchaseOrdersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateManualPurchaseOrderUiState())
    val uiState = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val createState = _createState.asStateFlow()

    fun addProducts(selected: List<QuoteCreationProductUi>) {
        if (selected.isEmpty()) return
        val current = _uiState.value.products.associateBy { it.id }.toMutableMap()
        selected.forEach { product ->
            val existing = current[product.id]
            if (existing != null) {
                current[product.id] = existing.copy(
                    quantity = (existing.quantity + product.quantity.coerceAtLeast(1))
                        .coerceAtLeast(1),
                    name = product.name.ifBlank { existing.name },
                    sku = product.sku.ifBlank { existing.sku },
                    unitCost = if (product.unitPrice > 0) product.unitPrice else existing.unitCost,
                    thumbnailLabel = product.thumbnailLabel.ifBlank { existing.thumbnailLabel },
                    brandThumbnail = product.brandThumbnail
                )
            } else {
                current[product.id] = ManualPoProductLineUi(
                    id = product.id,
                    name = product.name,
                    sku = product.sku,
                    thumbnailLabel = product.thumbnailLabel.ifBlank {
                        product.name.take(1).uppercase(Locale.US)
                    },
                    brandThumbnail = product.brandThumbnail,
                    quantity = product.quantity.coerceAtLeast(1),
                    uom = DEFAULT_UOM,
                    vendor = DEFAULT_VENDORS.first(),
                    vendors = DEFAULT_VENDORS,
                    unitCost = product.unitPrice.coerceAtLeast(0.0)
                )
            }
        }
        _uiState.value = _uiState.value.copy(
            products = current.values.toList(),
            showProductsValidation = false
        )
    }

    fun removeProduct(productId: String) {
        _uiState.value = _uiState.value.copy(
            products = _uiState.value.products.filterNot { it.id == productId },
            showProductsValidation = false
        )
    }

    fun updateProductQuantity(productId: String, quantityText: String) {
        val quantity = quantityText.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 1
        updateProduct(productId) { it.copy(quantity = quantity) }
    }

    fun selectProductVendor(productId: String, vendor: String) {
        updateProduct(productId) { it.copy(vendor = vendor) }
    }

    fun clearCreateState() {
        _createState.value = UiState.Idle
    }

    fun consumeProductsValidation() {
        if (_uiState.value.showProductsValidation) {
            _uiState.value = _uiState.value.copy(showProductsValidation = false)
        }
    }

    fun createPurchaseOrder() {
        val state = _uiState.value
        if (!state.hasProducts) {
            _uiState.value = state.copy(showProductsValidation = true)
            return
        }
        _createState.value = UiState.Loading
        viewModelScope.launch {
            val request = CreatePurchaseOrderRequest(
                products = state.products.map { line ->
                    AddPoProductLineUi(
                        id = line.id,
                        name = line.name,
                        axePart = line.sku,
                        maxQuantity = line.quantity,
                        quantity = line.quantity,
                        isSelected = true,
                        unitCost = line.unitCost,
                        vendor = line.vendor,
                        vendors = line.vendors,
                        uom = line.uom
                    )
                },
                subtotal = state.subtotal,
                tax = state.tax,
                shipping = state.shipping,
                grandTotal = state.grandTotal,
                isManual = true
            )
            when (val result = purchaseOrdersRepository.createPurchaseOrder(request)) {
                is ApiResult.Success -> {
                    _createState.value = UiState.Success(result.data.message)
                }
                is ApiResult.HttpError -> {
                    _createState.value = UiState.Error(result.message.ifBlank { FALLBACK_ERROR })
                }
                is ApiResult.NetworkError -> {
                    _createState.value = UiState.Error(result.message.ifBlank { FALLBACK_ERROR })
                }
                is ApiResult.UnknownError -> {
                    _createState.value = UiState.Error(result.message.ifBlank { FALLBACK_ERROR })
                }
                ApiResult.Unauthorized -> {
                    _createState.value = UiState.Error("Session expired. Please sign in again.")
                }
                else -> {
                    _createState.value = UiState.Error(FALLBACK_ERROR)
                }
            }
        }
    }

    private fun updateProduct(
        productId: String,
        transform: (ManualPoProductLineUi) -> ManualPoProductLineUi
    ) {
        _uiState.value = _uiState.value.copy(
            products = _uiState.value.products.map { product ->
                if (product.id == productId) transform(product) else product
            }
        )
    }

    companion object {
        private const val FALLBACK_ERROR = "Unable to create purchase order."
        private const val DEFAULT_UOM = "N/A"
        private val DEFAULT_VENDORS = listOf("TD_SYNNEX", "Ingram Micro", "Arrow Electronics")
    }
}
