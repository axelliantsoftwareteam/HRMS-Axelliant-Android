package com.axelliant.hris.features.purchaseorders.domain.model

import androidx.annotation.StringRes

enum class PurchaseOrderStatus {
    DRAFT,
    RELEASED,
    PENDING,
    CANCELED,
    APPROVED
}

enum class PurchaseOrderUtilization {
    PARTIALLY_UTILIZED,
    FULLY_UTILIZED,
    PENDING
}

data class PurchaseOrderModel(
    val id: String,
    val poNumber: String,
    val vendorName: String,
    val status: PurchaseOrderStatus,
    val utilization: PurchaseOrderUtilization,
    val grandTotal: String,
    val deliveryDate: String
)

data class PurchaseOrderProductLine(
    val id: String,
    val productId: String = "",
    val name: String,
    val description: String = "",
    val partNumber: String,
    val uom: String = "",
    val quantity: Int,
    val unitPrice: String,
    val lineTotal: String,
    val vendorId: String = "",
    val vendorName: String = "",
    val deliveryDate: String = "",
    val saleOrderDetailIds: List<String> = emptyList(),
    val quoteIds: List<String> = emptyList()
)

data class PurchaseOrderDetailModel(
    val id: String,
    val poNumber: String,
    val utilization: PurchaseOrderUtilization,
    val status: PurchaseOrderStatus,
    val grandTotal: String,
    val vendorName: String,
    val fulfillmentStatus: PurchaseOrderStatus,
    val createdBy: String,
    val createdDate: String,
    val billingAddress: String,
    val shippingAddress: String,
    val products: List<PurchaseOrderProductLine>,
    val billingAddressId: String = "",
    val shippingAddressId: String = "",
    val vendorId: String = "",
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double = 0.0,
    val quoteIds: List<String> = emptyList(),
    val saleOrderIds: List<String> = emptyList(),
    val quoteSerialIds: List<String> = emptyList(),
    val soSerialIds: List<String> = emptyList(),
    val customerEmail: String = "",
    val customerName: String = "",
    val billingAddressData: AddPoAddressUi? = null,
    val shippingAddressData: AddPoAddressUi? = null
)

data class PoVendorUi(
    val id: String,
    val name: String
)

data class PoAddressUi(
    val id: String,
    val label: String,
    val address: String,
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val zipCode: String = ""
) {
    val displayText: String
        get() = label.ifBlank {
            listOf(address, city, state)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" - ")
                .ifBlank { address }
        }
}

data class EditPoProductLineUi(
    val id: String,
    val productId: String = "",
    val name: String,
    val description: String = "",
    val sku: String,
    val uom: String = "",
    val quantity: Int,
    val unitCost: Double,
    val vendorId: String = "",
    val vendorName: String = "",
    val deliveryDate: String = "",
    val saleOrderDetailIds: List<String> = emptyList(),
    val quoteIds: List<String> = emptyList()
) {
    val lineTotal: Double
        get() = unitCost * quantity.coerceAtLeast(0)
}

data class UpdatePurchaseOrderRequest(
    val id: String,
    val poNumber: String,
    val vendorId: String,
    val vendorName: String,
    val billingAddressId: String,
    val billingAddressLabel: String,
    val shippingAddressId: String,
    val shippingAddressLabel: String,
    val quoteIds: List<String> = emptyList(),
    val saleOrderIds: List<String> = emptyList(),
    val quoteSerialIds: List<String> = emptyList(),
    val soSerialIds: List<String> = emptyList(),
    val customerEmail: String = "",
    val customerName: String = "",
    val billingAddress: AddPoAddressUi? = null,
    val shippingAddress: AddPoAddressUi? = null,
    val products: List<EditPoProductLineUi>,
    val subtotal: Double,
    val shipping: Double,
    val tax: Double,
    val taxRate: Double,
    val grandTotal: Double
)

data class PurchaseOrdersEmptyStateUi(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

data class PurchaseOrderListUiModel(
    val orders: List<PurchaseOrderModel>,
    val filterChips: List<PurchaseOrderUtilizationFilterChip> = emptyList(),
    val selectedFilterId: String = "all",
    val totalCount: Int = 0,
    val isLoadingNextPage: Boolean = false,
    val isLastPage: Boolean = false,
    val emptyState: PurchaseOrdersEmptyStateUi? = null
)

data class PurchaseOrderPageResult(
    val orders: List<PurchaseOrderModel>,
    val totalCount: Int
)

data class PurchaseOrderUtilizationFilterChip(
    val id: String,
    @StringRes val labelRes: Int,
    val utilization: PurchaseOrderUtilization?,
    val count: Int
)
