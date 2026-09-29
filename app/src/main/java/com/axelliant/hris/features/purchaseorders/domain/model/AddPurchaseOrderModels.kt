package com.axelliant.hris.features.purchaseorders.domain.model

data class SaleOrderDdlUi(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val amount: String,
    val quotationId: String = "",
    val quotationNumber: String = ""
)

data class AddPoProductLineUi(
    val id: String,
    val productId: String = "",
    val name: String,
    val description: String = "",
    val axePart: String,
    val maxQuantity: Int,
    val quantity: Int,
    val isSelected: Boolean,
    val unitCost: Double,
    val vendorId: String = "",
    val vendor: String,
    val vendors: List<String>,
    val uom: String,
    val saleOrderDetailIds: List<String> = emptyList(),
    val quoteIds: List<String> = emptyList(),
    val deliveryDate: String = ""
) {
    val lineTotal: Double
        get() = unitCost * quantity.coerceAtLeast(0)
}

data class AddPoAddressUi(
    val id: String,
    val accountId: String = "",
    val address: String = "",
    val country: String = "",
    val stateOrProvince: String = "",
    val city: String = "",
    val zip: String = "",
    val type: Int? = null,
    val isDiscarded: Boolean = false
)

data class AddPoSaleOrderInfoUi(
    val saleOrderId: String = "",
    val saleOrderNumber: String,
    val quoteNumber: String,
    val customerName: String,
    val customerEmail: String = "",
    val amount: String,
    val billingAddressId: String = "",
    val shippingAddressId: String = "",
    val billingAddress: AddPoAddressUi? = null,
    val shippingAddress: AddPoAddressUi? = null,
    val billingAddressLines: List<String>,
    val shippingAddressLines: List<String>,
    val products: List<AddPoProductLineUi>,
    val tax: Double,
    val shipping: Double
)

data class CreatePurchaseOrderRequest(
    val quotationId: String = "",
    val quotationNumber: String = "",
    val saleOrderId: String = "",
    val saleOrderNumber: String = "",
    val customerName: String = "",
    val products: List<AddPoProductLineUi>,
    val subtotal: Double,
    val tax: Double,
    val shipping: Double,
    val grandTotal: Double,
    val saleOrderInfo: AddPoSaleOrderInfoUi? = null,
    val isManual: Boolean = false
)

data class ManualPoProductLineUi(
    val id: String,
    val name: String,
    val sku: String,
    val thumbnailLabel: String,
    val brandThumbnail: Boolean,
    val quantity: Int,
    val uom: String,
    val vendor: String,
    val vendors: List<String>,
    val unitCost: Double
) {
    val lineTotal: Double
        get() = unitCost * quantity.coerceAtLeast(0)
}

data class CreatePurchaseOrderResult(
    val purchaseOrder: PurchaseOrderModel,
    val message: String
)
