package com.axelliant.hris.features.saleorders.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GetSaleOrdersRequest(
    @SerializedName("start")
    val start: Int = 0,
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

data class GetSaleOrdersResponse(
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("dataList")
    val dataList: List<SaleOrderListItemDto>? = null
)

data class SaleOrderListItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("soId")
    val soId: String? = null,
    @SerializedName("quotes")
    val quotes: String? = null,
    @SerializedName("subTotal")
    val subTotal: Double? = null,
    @SerializedName("grandTotal")
    val grandTotal: Double? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("utilizingStatus")
    val utilizingStatus: String? = null,
    @SerializedName("approvalStatus")
    val approvalStatus: Int? = null,
    @SerializedName("approvalStatusFlooring")
    val approvalStatusFlooring: Int? = null,
    @SerializedName("accountName")
    val accountName: String? = null,
    @SerializedName("quoteId")
    val quoteId: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class SaleOrderSingleDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("soId")
    val soId: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("subTotal")
    val subTotal: Double? = null,
    @SerializedName("taxAmount")
    val taxAmount: Double? = null,
    @SerializedName("grandTotal")
    val grandTotal: Double? = null,
    @SerializedName("salesOrderDetail")
    val salesOrderDetail: List<SaleOrderDetailLineDto>? = null,
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("paymentTermId")
    val paymentTermId: String? = null,
    @SerializedName("otherPaymentTerm")
    val otherPaymentTerm: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("priceProfileId")
    val priceProfileId: String? = null,
    @SerializedName("isQuoteRequired")
    val isQuoteRequired: Boolean? = null,
    @SerializedName("shippingType")
    val shippingType: Int? = null,
    @SerializedName("billingAddress")
    val billingAddress: SaleOrderAddressDto? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: SaleOrderAddressDto? = null,
    @SerializedName("quoteGrandTotal")
    val quoteGrandTotal: Double? = null,
    @SerializedName("paymentTermName")
    val paymentTermName: String? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("dealRegistrationId")
    val dealRegistrationId: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null,
    @SerializedName("customerSerialId")
    val customerSerialId: String? = null,
    @SerializedName("customerName")
    val customerName: String? = null,
    @SerializedName("validity")
    val validity: String? = null,
    @SerializedName("expiryDate")
    val expiryDate: String? = null,
    @SerializedName("isDealRegistration")
    val isDealRegistration: Boolean? = null,
    @SerializedName("isDealRegistrationId")
    val isDealRegistrationId: String? = null,
    @SerializedName("comments")
    val comments: List<SaleOrderCommentDto>? = null
)

data class SaleOrderDetailLineDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("salesOrderId")
    val salesOrderId: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("basePrice")
    val basePrice: Double? = null,
    @SerializedName("unitPrice")
    val unitPrice: Double? = null,
    @SerializedName("totalPrice")
    val totalPrice: Double? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("tax")
    val tax: Double? = null,
    @SerializedName("shipping")
    val shipping: Double? = null,
    @SerializedName("paymentTermId")
    val paymentTermId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("productName")
    val productName: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("uom")
    val uom: String? = null,
    @SerializedName("axePartNo")
    val axePartNo: String? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: SaleOrderAddressDto? = null,
    @SerializedName("productType")
    val productType: Int? = null,
    @SerializedName("serviceType")
    val serviceType: Int? = null,
    @SerializedName("subPlanId")
    val subPlanId: String? = null,
    @SerializedName("subPlanTierId")
    val subPlanTierId: String? = null,
    @SerializedName("billingFrequency")
    val billingFrequency: Int? = null,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null
)

data class SaleOrderAddressDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("address")
    val address: String? = null,
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
    val type: Int? = null
)

data class SaleOrderCommentDto(
    @SerializedName("comment")
    val comment: String? = null,
    @SerializedName("commentedBy")
    val commentedBy: String? = null,
    @SerializedName("commentedDate")
    val commentedDate: String? = null
)

data class AddEditSaleOrderRequest(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("shippingType")
    val shippingType: Int = SHIPPING_TYPE_STANDARD,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("quoteId")
    val quoteId: String? = null,
    @SerializedName("priceProfileId")
    val priceProfileId: String,
    @SerializedName("billingAddressId")
    val billingAddressId: String,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("soId")
    val soId: String = "",
    @SerializedName("paymentTermId")
    val paymentTermId: String,
    @SerializedName("otherPaymentTerm")
    val otherPaymentTerm: String = "",
    @SerializedName("expiryDate")
    val expiryDate: String? = null,
    @SerializedName("subTotal")
    val subTotal: Double,
    @SerializedName("taxAmount")
    val taxAmount: Double = 0.0,
    @SerializedName("grandTotal")
    val grandTotal: Double,
    @SerializedName("dealRegistrationId")
    val dealRegistrationId: String = "",
    @SerializedName("isQuoteRequired")
    val isQuoteRequired: Boolean = true,
    @SerializedName("salesOrderDetail")
    val salesOrderDetail: List<AddEditSaleOrderLineDto>,
    @SerializedName("status")
    val status: Int = STATUS_DRAFT,
    @SerializedName("deliveryDate")
    val deliveryDate: String
) {
    companion object {
        const val SHIPPING_TYPE_STANDARD = 1
        const val STATUS_DRAFT = 5
    }
}

data class AddEditSaleOrderLineDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("salesOrderId")
    val salesOrderId: String? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String,
    @SerializedName("productType")
    val productType: Int = PRODUCT_TYPE_STANDARD,
    @SerializedName("serviceType")
    val serviceType: Int? = null,
    @SerializedName("basePrice")
    val basePrice: Double,
    @SerializedName("paymentTermId")
    val paymentTermId: String,
    @SerializedName("otherPaymentTerm")
    val otherPaymentTerm: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("unitPrice")
    val unitPrice: Double,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("totalPrice")
    val totalPrice: Double,
    @SerializedName("flagged")
    val flagged: Boolean = true,
    @SerializedName("tenureType")
    val tenureType: String? = null,
    @SerializedName("calculationApplied")
    val calculationApplied: String? = null,
    @SerializedName("subPlanId")
    val subPlanId: String? = null,
    @SerializedName("subPlanTierId")
    val subPlanTierId: String? = null,
    @SerializedName("billingFrequency")
    val billingFrequency: Int? = null,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null
) {
    companion object {
        const val PRODUCT_TYPE_STANDARD = 1
    }
}

data class AddEditSaleOrderResponseDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("soId")
    val soId: String? = null,
    @SerializedName("message")
    val message: String? = null
)
