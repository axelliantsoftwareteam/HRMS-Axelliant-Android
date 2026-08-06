package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountDdlDto(
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("customerFirstName")
    val customerFirstName: String? = null,
    @SerializedName("customerEmail")
    val customerEmail: String? = null,
    @SerializedName("isPriceProfileIdExist")
    val isPriceProfileIdExist: Boolean? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("catalog")
    val catalog: String? = null,
    @SerializedName("ae")
    val ae: String? = null,
    @SerializedName("typeOfCustomer")
    val typeOfCustomer: String? = null
)

data class CustomerQuotationDetailsDto(
    @SerializedName("priceProfileId")
    val priceProfileId: String? = null,
    @SerializedName("priceProfileName")
    val priceProfileName: String? = null,
    @SerializedName("creditStatus")
    val creditStatus: Boolean? = null,
    @SerializedName("currentBalance")
    val currentBalance: Double? = null,
    @SerializedName("billingAddress")
    val billingAddress: List<CustomerAddressDto>? = null,
    @SerializedName("shippingAddress")
    val shippingAddress: List<CustomerAddressDto>? = null
)

data class CustomerAddressDto(
    @SerializedName("billingAddressId")
    val billingAddressId: String? = null,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String? = null,
    @SerializedName("addressId")
    val addressId: String? = null,
    @SerializedName("id")
    val id: String? = null,
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

data class PaymentTermDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("paymentTermType")
    val paymentTermType: String? = null,
    @SerializedName("status")
    val status: String? = null
)

data class AddEditQuotationRequest(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("IsQuoteRevision")
    val isQuoteRevision: Boolean = false,
    @SerializedName("quoteName")
    val quoteName: String,
    @SerializedName("accountId")
    val accountId: String,
    @SerializedName("serviceType")
    val serviceType: Int = SERVICE_TYPE_STANDARD,
    @SerializedName("priceProfileId")
    val priceProfileId: String,
    @SerializedName("billingAddressId")
    val billingAddressId: String,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("paymentTermId")
    val paymentTermId: String,
    @SerializedName("otherPaymentTerm")
    val otherPaymentTerm: String = "",
    @SerializedName("availableCreditAtCreation")
    val availableCreditAtCreation: Double? = null,
    @SerializedName("isCreditHold")
    val isCreditHold: Boolean = false,
    @SerializedName("expiryDate")
    val expiryDate: String? = null,
    @SerializedName("subTotal")
    val subTotal: Double = 0.0,
    @SerializedName("taxAmount")
    val taxAmount: Double = 0.0,
    @SerializedName("grandTotal")
    val grandTotal: Double = 0.0,
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("isDealRegistration")
    val isDealRegistration: Boolean = false,
    @SerializedName("isDealRegistrationId")
    val isDealRegistrationId: String = "",
    @SerializedName("dealRegistrationAtt")
    val dealRegistrationAtt: String = "",
    @SerializedName("quoteAttachment")
    val quoteAttachment: List<Any> = emptyList(),
    @SerializedName("status")
    val status: Int = QUOTATION_STATUS_DRAFT,
    @SerializedName("lineItems")
    val lineItems: List<AddEditQuotationLineItemDto> = emptyList(),
    @SerializedName("parentDeliveryDate")
    val parentDeliveryDate: String? = null
) {
    companion object {
        const val QUOTATION_STATUS_DRAFT = 5
        const val SERVICE_TYPE_STANDARD = 2
    }
}

data class AddEditQuotationLineItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("quoteId")
    val quoteId: String? = null,
    @SerializedName("productId")
    val productId: String,
    @SerializedName("productType")
    val productType: Int = PRODUCT_TYPE_STANDARD,
    @SerializedName("basePrice")
    val basePrice: Double,
    @SerializedName("unitPrice")
    val unitPrice: Double,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("lineTotal")
    val lineTotal: Double,
    @SerializedName("flagged")
    val flagged: Boolean = true,
    @SerializedName("tenureType")
    val tenureType: String? = null,
    @SerializedName("calculationApplied")
    val calculationApplied: String? = null,
    @SerializedName("isCustomDeliveryDate")
    val isCustomDeliveryDate: Boolean = false,
    @SerializedName("customDeliveryDates")
    val customDeliveryDates: List<AddEditQuotationDeliveryScheduleDto> = emptyList()
) {
    companion object {
        const val PRODUCT_TYPE_STANDARD = 1
    }
}

data class AddEditQuotationDeliveryScheduleDto(
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("deliveryDate")
    val deliveryDate: String,
    @SerializedName("shippingAddressId")
    val shippingAddressId: String,
    @SerializedName("paymentTermId")
    val paymentTermId: String? = null,
    @SerializedName("otherPaymentTerm")
    val otherPaymentTerm: String? = null,
    @SerializedName("shipping")
    val shipping: Double = 0.0,
    @SerializedName("tax")
    val tax: Double = 0.0,
    @SerializedName("shippingAddress")
    val shippingAddress: Map<String, Any> = emptyMap(),
    @SerializedName("serialId")
    val serialId: String? = null
)

data class AddEditQuotationResponseDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("quotationId")
    val quotationId: String? = null,
    @SerializedName("quoteSerialNo")
    val quoteSerialNo: String? = null,
    @SerializedName("message")
    val message: String? = null
)
