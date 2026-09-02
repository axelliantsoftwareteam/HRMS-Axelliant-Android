package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class QuoteReportResponse(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("quoteSerialId")
    val quoteSerialId: String? = null,
    @SerializedName("quoteCreatedOn")
    val quoteCreatedOn: Long? = null,
    @SerializedName("quoteCreatedDate")
    val quoteCreatedDate: String? = null,
    @SerializedName("customerSerialId")
    val customerSerialId: String? = null,
    @SerializedName("serviceType")
    val serviceType: String? = null,
    @SerializedName("signatureImage")
    val signatureImage: String? = null,
    @SerializedName("axelliantAddress")
    val axelliantAddress: String? = null,
    @SerializedName("axelliantPhone")
    val axelliantPhone: String? = null,
    @SerializedName("footerAddress")
    val footerAddress: QuoteReportFooterAddressDto? = null,
    @SerializedName("tagLine")
    val tagLine: String? = null,
    @SerializedName("billingAddress")
    val billingAddress: QuoteAddressDto? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: QuoteAddressDto? = null,
    @SerializedName("quoteTile")
    val quoteTitle: String? = null,
    @SerializedName("paymentTerm")
    val paymentTerm: String? = null,
    @SerializedName("shippingService")
    val shippingService: String? = null,
    @SerializedName("weight")
    val weight: Double? = null,
    @SerializedName("signatureOptions")
    val signatureOptions: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("ae")
    val ae: String? = null,
    @SerializedName("subTotal")
    val subTotal: Double? = null,
    @SerializedName("taxAmount")
    val taxAmount: Double? = null,
    @SerializedName("shipping")
    val shipping: Double? = null,
    @SerializedName("recycleFee")
    val recycleFee: Double? = null,
    @SerializedName("grandTotal")
    val grandTotal: Double? = null,
    @SerializedName("customerName")
    val customerName: String? = null,
    @SerializedName("customerEmail")
    val customerEmail: String? = null,
    @SerializedName("customerPhone")
    val customerPhone: String? = null,
    @SerializedName("customerDate")
    val customerDate: String? = null,
    @SerializedName("po")
    val po: String? = null,
    @SerializedName("signature")
    val signature: String? = null,
    @SerializedName("lineItems")
    val lineItems: List<QuoteReportLineItemDto>? = null,
    @SerializedName("status")
    val status: Int? = null
)

data class QuoteReportFooterAddressDto(
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("zip")
    val zip: String? = null,
    @SerializedName("country")
    val country: String? = null
)

data class QuoteReportLineItemDto(
    @SerializedName("manufacturer")
    val manufacturer: String? = null,
    @SerializedName("mfgPartNo")
    val mfgPartNo: String? = null,
    @SerializedName("axePartNo")
    val axePartNo: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("basePrice")
    val basePrice: Double? = null,
    @SerializedName("unitPrice")
    val unitPrice: Double? = null,
    @SerializedName("lineTotal")
    val lineTotal: Double? = null,
    @SerializedName("tax")
    val tax: Double? = null,
    @SerializedName("uniqueNumber")
    val uniqueNumber: String? = null,
    @SerializedName("duration")
    val duration: String? = null,
    @SerializedName("showDuration")
    val showDuration: Boolean? = null
)
