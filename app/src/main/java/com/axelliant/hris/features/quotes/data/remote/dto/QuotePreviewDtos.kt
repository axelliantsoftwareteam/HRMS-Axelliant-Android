package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class QuotePreviewResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("quotationId")
    val quotationId: String? = null,
    @SerializedName("quoteName")
    val quoteName: String? = null,
    @SerializedName("quoteSerialNo")
    val quoteSerialNo: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("accountName")
    val accountName: String? = null,
    @SerializedName("quotationType")
    val quotationType: String? = null,
    @SerializedName("quoteSource")
    val quoteSource: String? = null,
    @SerializedName("approvalStatus")
    val approvalStatus: String? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdByEmail")
    val createdByEmail: String? = null,
    @SerializedName("ae")
    val ae: String? = null,
    @SerializedName("acountName")
    val acountName: String? = null,
    @SerializedName("customerEmail")
    val customerEmail: String? = null,
    @SerializedName("accountSerialNo")
    val accountSerialNo: String? = null,
    @SerializedName("priceProfileId")
    val priceProfileId: String? = null,
    @SerializedName("priceProfileName")
    val priceProfileName: String? = null,
    @SerializedName("creditStatus")
    val creditStatus: Boolean? = null,
    @SerializedName("currentBalance")
    val currentBalance: Double? = null,
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("paymentTermId")
    val paymentTermId: String? = null,
    @SerializedName("paymentTerm")
    val paymentTerm: String? = null,
    @SerializedName("parentDeliveryDate")
    val parentDeliveryDate: String? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("isDealRegistration")
    val isDealRegistration: Boolean? = null,
    @SerializedName("dealRegistration")
    val dealRegistration: Boolean? = null,
    @SerializedName("billingAddress")
    val billingAddress: QuoteAddressDto? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: QuoteAddressDto? = null,
    @SerializedName("lineItems")
    val lineItems: List<QuoteLineItemDto>? = null,
    @SerializedName("isCreditHold")
    val isCreditHold: Boolean? = null,
    @SerializedName("subTotal")
    val subTotal: Double? = null,
    @SerializedName("taxAmount")
    val taxAmount: Double? = null,
    @SerializedName("shipping")
    val shipping: Double? = null,
    @SerializedName("grandTotal")
    val grandTotal: Double? = null,
    @SerializedName("comments")
    val comments: List<QuoteCommentDto>? = null
)

data class QuoteAddressDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("addressId")
    val addressId: String? = null,
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("stateOrProvince")
    val stateOrProvince: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("zip")
    val zip: String? = null,
    @SerializedName("zipCode")
    val zipCode: String? = null
)

data class QuoteLineItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("axePartNo")
    val axePartNo: String? = null,
    @SerializedName("sku")
    val sku: String? = null,
    @SerializedName("productName")
    val productName: String? = null,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("unitPrice")
    val unitPrice: Double? = null,
    @SerializedName("basePrice")
    val basePrice: Double? = null,
    @SerializedName("lineTotal")
    val lineTotal: Double? = null,
    @SerializedName("customDeliveryDates")
    val customDeliveryDates: List<QuoteCustomDeliveryDateDto>? = null,
    @SerializedName("product")
    val product: QuoteLineItemProductDto? = null
)

data class QuoteLineItemProductDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("axePartNo")
    val axePartNo: String? = null,
    @SerializedName("mfgPartNo")
    val mfgPartNo: String? = null
)

data class QuoteCustomDeliveryDateDto(
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("deliveryDate")
    val deliveryDate: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: QuoteAddressDto? = null
)

data class QuoteCommentDto(
    @SerializedName("comment")
    val comment: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)
