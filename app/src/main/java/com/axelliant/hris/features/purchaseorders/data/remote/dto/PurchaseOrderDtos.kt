package com.axelliant.hris.features.purchaseorders.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GetPurchaseOrdersRequest(
    @SerializedName("start")
    val start: Int = 1,
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("sort")
    val sort: String = "",
    @SerializedName("order")
    val order: String = "",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = "",
    @SerializedName("filter")
    val filter: List<Any> = emptyList()
)

data class GetPurchaseOrdersResponse(
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("dataList")
    val dataList: List<PurchaseOrderListItemDto>? = null
)

data class PurchaseOrderListItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("poId")
    val poId: String? = null,
    @SerializedName("quotes")
    val quotes: String? = null,
    @SerializedName("totalAmount")
    val totalAmount: Double? = null,
    @SerializedName("vendorName")
    val vendorName: String? = null,
    @SerializedName("utilizingStatus")
    val utilizingStatus: String? = null,
    @SerializedName("approvalStatus")
    val approvalStatus: Int? = null,
    @SerializedName("warehouseStatus")
    val warehouseStatus: Int? = null,
    @SerializedName("revision")
    val revision: Int? = null,
    @SerializedName("isManual")
    val isManual: Boolean? = null,
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("serialNo")
    val serialNo: Int? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class GetSinglePurchaseOrderResponse(
    @SerializedName("masterData")
    val masterData: List<PurchaseOrderSingleMasterDto>? = null,
    @SerializedName("purchaseOrderDetail")
    val purchaseOrderDetail: List<PurchaseOrderSingleDetailDto>? = null,
    @SerializedName("quoteDDL")
    val quoteDdl: List<PurchaseOrderDdlDto>? = null,
    @SerializedName("saleOrderDDL")
    val saleOrderDdl: List<PurchaseOrderDdlDto>? = null
)

data class PurchaseOrderSingleMasterDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("poId")
    val poId: String? = null,
    @SerializedName("quoteIds")
    val quoteIds: List<String>? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("subTotal")
    val subTotal: Double? = null,
    @SerializedName("shipping")
    val shipping: Double? = null,
    @SerializedName("tax")
    val tax: Double? = null,
    @SerializedName("totalAmount")
    val totalAmount: Double? = null,
    @SerializedName("purchaseOrderDetail")
    val purchaseOrderDetail: List<PurchaseOrderSingleDetailDto>? = null,
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("customerEmail")
    val customerEmail: String? = null,
    @SerializedName("customerName")
    val customerName: String? = null,
    @SerializedName("billingAddress")
    val billingAddress: PurchaseOrderSingleAddressDto? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: PurchaseOrderSingleAddressDto? = null,
    @SerializedName("quoteSerialIds")
    val quoteSerialIds: List<String>? = null,
    @SerializedName("soSerialIds")
    val soSerialIds: List<String>? = null,
    @SerializedName("approvalStatus")
    val approvalStatus: Int? = null
)

data class PurchaseOrderSingleDetailDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("purchaseOrderId")
    val purchaseOrderId: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("unitPrice")
    val unitPrice: Double? = null,
    @SerializedName("totalPrice")
    val totalPrice: Double? = null,
    @SerializedName("vendorId")
    val vendorId: String? = null,
    @SerializedName("saleOrderDetailIds")
    val saleOrderDetailIds: List<String>? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("quoteIds")
    val quoteIds: List<String>? = null,
    @SerializedName("productName")
    val productName: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("vendorName")
    val vendorName: String? = null,
    @SerializedName("uom")
    val uom: String? = null,
    @SerializedName("axePartNo")
    val axePartNo: String? = null,
    @SerializedName("productType")
    val productType: Int? = null
)

data class PurchaseOrderSingleAddressDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("contact")
    val contact: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("stateOrProvince")
    val stateOrProvince: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("zip")
    val zip: String? = null,
    @SerializedName("zipCode")
    val zipCode: String? = null,
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("isDiscarded")
    val isDiscarded: Boolean? = null
)

data class PurchaseOrderDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("text")
    val text: String? = null
)

data class VendorDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("vendorId")
    val vendorId: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("vendorName")
    val vendorName: String? = null,
    @SerializedName("code")
    val code: String? = null
)

data class CompanyAddressesDto(
    @SerializedName("billingAddress")
    val billingAddress: List<CompanyAddressDto>? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: List<CompanyAddressDto>? = null
)

data class CompanyAddressDto(
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("addressId")
    val addressId: String? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("zip")
    val zip: String? = null,
    @SerializedName("zipCode")
    val zipCode: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("stateOrProvince")
    val stateOrProvince: String? = null,
    @SerializedName("country")
    val country: String? = null
)

data class AddEditPurchaseOrderRequest(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("poNumber")
    val poNumber: String? = null,
    @SerializedName("vendorId")
    val vendorId: String,
    @SerializedName("billingAddressId")
    val billingAddressId: String,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("subTotal")
    val subTotal: Double = 0.0,
    @SerializedName("shippingAmount")
    val shippingAmount: Double = 0.0,
    @SerializedName("taxAmount")
    val taxAmount: Double = 0.0,
    @SerializedName("grandTotal")
    val grandTotal: Double = 0.0,
    @SerializedName("taxRate")
    val taxRate: Double = DEFAULT_TAX_RATE,
    @SerializedName("lineItems")
    val lineItems: List<AddEditPurchaseOrderLineDto> = emptyList()
) {
    companion object {
        const val DEFAULT_TAX_RATE = 0.085
    }
}

data class AddEditPurchaseOrderLineDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("sku")
    val sku: String = "",
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("unitCost")
    val unitCost: Double,
    @SerializedName("lineTotal")
    val lineTotal: Double
)

data class AddEditPurchaseOrderResponseDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("poNumber")
    val poNumber: String? = null,
    @SerializedName("poId")
    val poId: String? = null,
    @SerializedName("message")
    val message: String? = null
)

data class AddEditPosPurchaseOrderRequest(
    @SerializedName("masterData")
    val masterData: List<AddEditPosPurchaseOrderMasterDto>,
    @SerializedName("purchaseOrderDetail")
    val purchaseOrderDetail: List<AddEditPosPurchaseOrderDetailDto>
)

data class AddEditPosPurchaseOrderMasterDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("poId")
    val poId: String = "",
    @SerializedName("quoteIds")
    val quoteIds: List<String>,
    @SerializedName("status")
    val status: Int = STATUS_DRAFT,
    @SerializedName("subTotal")
    val subTotal: Double,
    @SerializedName("shipping")
    val shipping: Double,
    @SerializedName("tax")
    val tax: Double,
    @SerializedName("totalAmount")
    val totalAmount: Double,
    @SerializedName("billingAddressId")
    val billingAddressId: String,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("saleOrderId")
    val saleOrderId: List<String>,
    @SerializedName("customerEmail")
    val customerEmail: String,
    @SerializedName("customerName")
    val customerName: String,
    @SerializedName("billingAddress")
    val billingAddress: AddEditPosAddressDto?,
    @SerializedName("shippingAddress")
    val shippingAddress: AddEditPosAddressDto?,
    @SerializedName("quoteSerialIds")
    val quoteSerialIds: List<String> = emptyList(),
    @SerializedName("soSerialIds")
    val soSerialIds: List<String>
) {
    companion object {
        const val STATUS_DRAFT = 1
    }
}

data class AddEditPosAddressDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("accountId")
    val accountId: String = "",
    @SerializedName("address")
    val address: String = "",
    @SerializedName("country")
    val country: String = "",
    @SerializedName("stateOrProvince")
    val stateOrProvince: String = "",
    @SerializedName("city")
    val city: String = "",
    @SerializedName("zip")
    val zip: String = "",
    @SerializedName("type")
    val type: Int? = null,
    @SerializedName("isDiscarded")
    val isDiscarded: Boolean = false
)

data class AddEditPosPurchaseOrderDetailDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("purchaseOrderId")
    val purchaseOrderId: String? = null,
    @SerializedName("productId")
    val productId: String?,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("unitPrice")
    val unitPrice: Double,
    @SerializedName("totalPrice")
    val totalPrice: Double,
    @SerializedName("vendorId")
    val vendorId: String,
    @SerializedName("productName")
    val productName: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("vendorName")
    val vendorName: String,
    @SerializedName("uom")
    val uom: String,
    @SerializedName("axePartNo")
    val axePartNo: String,
    @SerializedName("saleOrderDetailIds")
    val saleOrderDetailIds: List<String>,
    @SerializedName("quoteIds")
    val quoteIds: List<String> = emptyList(),
    @SerializedName("deliveryDate")
    val deliveryDate: String
)
