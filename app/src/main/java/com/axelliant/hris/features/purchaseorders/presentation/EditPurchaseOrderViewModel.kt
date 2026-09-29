package com.axelliant.hris.features.purchaseorders.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.EditPoProductLineUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoVendorUi
import com.axelliant.hris.features.purchaseorders.domain.model.UpdatePurchaseOrderRequest
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditPurchaseOrderViewModel @Inject constructor(
    private val repository: PurchaseOrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val purchaseOrderId: String =
        savedStateHandle.get<String>(ARG_PURCHASE_ORDER_ID).orEmpty()

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private val _uiState = MutableStateFlow(EditPurchaseOrderUiState())
    val uiState = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        loadScreen()
    }

    fun selectVendor(vendor: PoVendorUi) {
        _uiState.update { it.copy(selectedVendor = vendor) }
    }

    fun selectBillingAddress(address: PoAddressUi) {
        _uiState.update { it.copy(selectedBillingAddress = address) }
    }

    fun selectShippingAddress(address: PoAddressUi) {
        _uiState.update { it.copy(selectedShippingAddress = address) }
    }

    fun addAddress(isBilling: Boolean, address: PoAddressUi) {
        _uiState.update { state ->
            if (isBilling) {
                state.copy(
                    billingAddresses = (state.billingAddresses + address).distinctBy { it.id },
                    selectedBillingAddress = address
                )
            } else {
                state.copy(
                    shippingAddresses = (state.shippingAddresses + address).distinctBy { it.id },
                    selectedShippingAddress = address
                )
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map { line ->
                    if (line.id == productId) line.copy(quantity = quantity.coerceAtLeast(0)) else line
                }
            ).recalculate()
        }
    }

    fun updateUnitCost(productId: String, unitCost: Double) {
        _uiState.update { state ->
            state.copy(
                products = state.products.map { line ->
                    if (line.id == productId) line.copy(unitCost = unitCost.coerceAtLeast(0.0)) else line
                }
            ).recalculate()
        }
    }

    fun updateShipping(shipping: Double) {
        _uiState.update { it.copy(shipping = shipping.coerceAtLeast(0.0)).recalculate() }
    }

    fun removeProduct(productId: String) {
        _uiState.update { state ->
            state.copy(products = state.products.filterNot { it.id == productId }).recalculate()
        }
    }

    fun addProducts(products: List<QuoteCreationProductUi>) {
        if (products.isEmpty()) return
        _uiState.update { state ->
            val existingIds = state.products.map { it.id }.toSet()
            val additions = products
                .filterNot { it.id in existingIds }
                .map { product ->
                    EditPoProductLineUi(
                        id = product.id,
                        productId = product.id,
                        name = product.name.ifBlank { product.sku },
                        sku = product.sku,
                        quantity = product.quantity.coerceAtLeast(1),
                        unitCost = product.unitPrice.coerceAtLeast(0.0)
                    )
                }
            state.copy(products = state.products + additions).recalculate()
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        if (!state.hasChanges) return
        val vendor = state.selectedVendor
        val billing = state.selectedBillingAddress
        val shipping = state.selectedShippingAddress
        if (vendor == null || billing == null || shipping == null) {
            _saveState.value = UiState.Error("Vendor and addresses are required.")
            return
        }
        if (state.products.isEmpty()) {
            _saveState.value = UiState.Error("Add at least one product.")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            val request = UpdatePurchaseOrderRequest(
                id = state.purchaseOrderId,
                poNumber = state.poNumber,
                vendorId = vendor.id,
                vendorName = vendor.name,
                billingAddressId = billing.id,
                billingAddressLabel = billing.displayText,
                shippingAddressId = shipping.id,
                shippingAddressLabel = shipping.displayText,
                quoteIds = state.quoteIds,
                saleOrderIds = state.saleOrderIds,
                quoteSerialIds = state.quoteSerialIds,
                soSerialIds = state.soSerialIds,
                customerEmail = state.customerEmail,
                customerName = state.customerName,
                billingAddress = state.resolveBillingAddress(),
                shippingAddress = state.resolveShippingAddress(),
                products = state.products,
                subtotal = state.subtotal,
                shipping = state.shipping,
                tax = state.tax,
                taxRate = state.taxRate,
                grandTotal = state.grandTotal
            )
            _saveState.value = when (val result = repository.updatePurchaseOrder(request)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Empty -> UiState.Error("Unable to save purchase order.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                is ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = UiState.Idle
    }

    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatCurrencyInput(amount: Double): String {
        return String.format(Locale.US, "$ %.2f", amount)
    }

    private fun loadScreen() {
        if (purchaseOrderId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Purchase order not found.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val detailResult = repository.getPurchaseOrderDetail(purchaseOrderId)
            val vendorsResult = repository.getVendors()
            val addressesResult = repository.getCompanyAddresses()

            if (detailResult !is ApiResult.Success) {
                val message = when (detailResult) {
                    is ApiResult.HttpError -> detailResult.message
                    is ApiResult.NetworkError -> detailResult.message
                    is ApiResult.UnknownError -> detailResult.message
                    else -> "Unable to load purchase order."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                return@launch
            }

            val detail = detailResult.data
            val vendors = (vendorsResult as? ApiResult.Success)?.data.orEmpty()
            val (remoteBilling, remoteShipping) = (addressesResult as? ApiResult.Success)?.data
                ?: (emptyList<PoAddressUi>() to emptyList())

            val selectedVendor = vendors.firstOrNull {
                it.id.equals(detail.vendorId, ignoreCase = true) ||
                it.name.equals(detail.vendorName, ignoreCase = true) ||
                    it.id.equals(detail.vendorName, ignoreCase = true)
            } ?: vendors.firstOrNull()

            val selectedBilling = remoteBilling.firstOrNull {
                it.id.equals(detail.billingAddressId, ignoreCase = true) ||
                detail.billingAddress.contains(it.address, ignoreCase = true) ||
                    detail.billingAddress.contains(it.label, ignoreCase = true)
            } ?: detail.billingAddress.toPoAddress(detail.billingAddressId)
                ?: remoteBilling.firstOrNull()

            val selectedShipping = remoteShipping.firstOrNull {
                it.id.equals(detail.shippingAddressId, ignoreCase = true) ||
                detail.shippingAddress.contains(it.address, ignoreCase = true) ||
                    detail.shippingAddress.contains(it.label, ignoreCase = true)
            } ?: detail.shippingAddress.toPoAddress(detail.shippingAddressId)
                ?: remoteShipping.firstOrNull()

            val billing = (remoteBilling + listOfNotNull(selectedBilling)).distinctBy { it.id }
            val shipping = (remoteShipping + listOfNotNull(selectedShipping)).distinctBy { it.id }

            val products = repository.toEditProducts(detail)
            val subtotal = products.sumOf { it.lineTotal }
            val shippingAmount = detail.shipping
            val tax = detail.tax

            _uiState.value = EditPurchaseOrderUiState(
                isLoading = false,
                purchaseOrderId = detail.id,
                poNumber = detail.poNumber,
                vendors = vendors,
                billingAddresses = billing,
                shippingAddresses = shipping,
                selectedVendor = selectedVendor,
                selectedBillingAddress = selectedBilling,
                selectedShippingAddress = selectedShipping,
                products = products,
                shipping = shippingAmount,
                taxRate = if (subtotal > 0.0) tax / subtotal else AddEditPurchaseOrderRequest.DEFAULT_TAX_RATE,
                subtotal = subtotal,
                tax = tax,
                grandTotal = subtotal + shippingAmount + tax,
                quoteIds = detail.quoteIds,
                saleOrderIds = detail.saleOrderIds,
                quoteSerialIds = detail.quoteSerialIds,
                soSerialIds = detail.soSerialIds,
                customerEmail = detail.customerEmail,
                customerName = detail.customerName,
                billingAddressData = detail.billingAddressData,
                shippingAddressData = detail.shippingAddressData
            )
            _uiState.value = _uiState.value.copy(
                initialSnapshot = _uiState.value.toSnapshot()
            )
        }
    }

    private fun EditPurchaseOrderUiState.recalculate(): EditPurchaseOrderUiState {
        val subtotal = products.sumOf { it.lineTotal }
        val tax = subtotal * taxRate
        return copy(
            subtotal = subtotal,
            tax = tax,
            grandTotal = subtotal + shipping + tax
        )
    }

    private fun String.toPoAddress(id: String): PoAddressUi? {
        val label = trim().takeIf { it.isNotBlank() && it != "N/A" } ?: return null
        return PoAddressUi(
            id = id.ifBlank { label },
            label = label,
            address = label
        )
    }

    private fun EditPurchaseOrderUiState.resolveBillingAddress(): AddPoAddressUi? {
        val selected = selectedBillingAddress ?: return billingAddressData
        return if (billingAddressData?.id == selected.id) {
            billingAddressData
        } else {
            selected.toAddPoAddress(type = 2)
        }
    }

    private fun EditPurchaseOrderUiState.resolveShippingAddress(): AddPoAddressUi? {
        val selected = selectedShippingAddress ?: return shippingAddressData
        return if (shippingAddressData?.id == selected.id) {
            shippingAddressData
        } else {
            selected.toAddPoAddress(type = 3)
        }
    }

    private fun PoAddressUi.toAddPoAddress(type: Int): AddPoAddressUi {
        return AddPoAddressUi(
            id = id,
            address = address,
            country = country,
            stateOrProvince = state,
            city = city,
            zip = zipCode,
            type = type,
            isDiscarded = false
        )
    }

    companion object {
        const val ARG_PURCHASE_ORDER_ID = "purchaseOrderId"
    }
}

data class EditPurchaseOrderUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val purchaseOrderId: String = "",
    val poNumber: String = "",
    val vendors: List<PoVendorUi> = emptyList(),
    val billingAddresses: List<PoAddressUi> = emptyList(),
    val shippingAddresses: List<PoAddressUi> = emptyList(),
    val selectedVendor: PoVendorUi? = null,
    val selectedBillingAddress: PoAddressUi? = null,
    val selectedShippingAddress: PoAddressUi? = null,
    val products: List<EditPoProductLineUi> = emptyList(),
    val shipping: Double = 0.0,
    val taxRate: Double = AddEditPurchaseOrderRequest.DEFAULT_TAX_RATE,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val quoteIds: List<String> = emptyList(),
    val saleOrderIds: List<String> = emptyList(),
    val quoteSerialIds: List<String> = emptyList(),
    val soSerialIds: List<String> = emptyList(),
    val customerEmail: String = "",
    val customerName: String = "",
    val billingAddressData: AddPoAddressUi? = null,
    val shippingAddressData: AddPoAddressUi? = null,
    val initialSnapshot: EditPurchaseOrderSnapshot? = null
) {
    val hasChanges: Boolean
        get() = !isLoading && errorMessage == null && initialSnapshot != null &&
            toSnapshot() != initialSnapshot

    fun toSnapshot(): EditPurchaseOrderSnapshot {
        return EditPurchaseOrderSnapshot(
            vendorId = selectedVendor?.id.orEmpty(),
            billingAddressId = selectedBillingAddress?.id.orEmpty(),
            shippingAddressId = selectedShippingAddress?.id.orEmpty(),
            products = products,
            shipping = shipping,
            taxRate = taxRate
        )
    }
}

data class EditPurchaseOrderSnapshot(
    val vendorId: String,
    val billingAddressId: String,
    val shippingAddressId: String,
    val products: List<EditPoProductLineUi>,
    val shipping: Double,
    val taxRate: Double
)
