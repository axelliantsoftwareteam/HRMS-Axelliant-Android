package com.axelliant.hris.features.quotes.domain.model

data class QuoteCustomerUi(
    val id: String,
    val name: String,
    val email: String = "",
    val customerFirstName: String = "",
    val isPriceProfileIdExist: Boolean = false,
    val catalog: String = "",
    val typeOfCustomer: String = "",
    val creditHoldEnabled: Boolean? = null,
    val remainingCredit: String = "",
    val accountExecutive: String = "",
    val priceProfile: String = "",
    val priceProfileId: String = "",
    val billingAddresses: List<QuoteAddressUi> = emptyList(),
    val shippingAddresses: List<QuoteAddressUi> = emptyList()
) {
    val creditHoldLabel: String
        get() = when (creditHoldEnabled) {
            true -> "Enabled"
            false -> "Disabled"
            null -> ""
        }
}

data class QuoteAddressUi(
    val id: String,
    val address: String,
    val country: String,
    val state: String,
    val city: String,
    val zipCode: String
) {
    val displayText: String
        get() = listOf(address, zipCode)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

    val displayTextWithLocation: String
        get() = listOf(address, city, state, country, zipCode)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
}

data class QuotePaymentTermUi(
    val id: String,
    val name: String,
    val description: String = "",
    val paymentTermType: String = "",
    val status: String = ""
)

data class QuoteProductDeliveryScheduleUi(
    val id: String,
    val shippingAddress: QuoteAddressUi,
    val quantity: Int = 1,
    val estimatedDeliveryDate: String = "",
    val isDefaultAddress: Boolean = false
)

data class QuoteCreationProductUi(
    val id: String,
    val lineItemId: String? = null,
    val name: String,
    val sku: String,
    val category: String,
    val thumbnailLabel: String,
    val brandThumbnail: Boolean,
    val unitPrice: Double,
    val basePrice: Double? = null,
    val quantity: Int = 1,
    val deliverySchedules: List<QuoteProductDeliveryScheduleUi> = emptyList()
) {
    val effectiveBasePrice: Double
        get() = basePrice ?: unitPrice

    val lineTotal: Double
        get() = unitPrice * quantity

    val scheduledQuantity: Int
        get() = deliverySchedules.sumOf { it.quantity }

    val overScheduledUnits: Int
        get() = (scheduledQuantity - quantity).coerceAtLeast(0)

    val isOverScheduled: Boolean
        get() = scheduledQuantity > quantity
}

data class CreateDraftQuoteRequest(
    val quoteId: String? = null,
    val quoteTitle: String,
    val customer: QuoteCustomerUi,
    val billingAddress: QuoteAddressUi,
    val shippingAddress: QuoteAddressUi,
    val paymentTerm: QuotePaymentTermUi,
    val deliveryDate: String,
    val products: List<QuoteCreationProductUi>,
    val dealRegistration: Boolean,
    val isWarehouse: Boolean = true
)

data class CreateDraftQuoteResult(
    val quote: QuoteModel,
    val message: String
)

data class SubmitQuoteResult(
    val message: String
)

data class CancelQuoteResult(
    val message: String
)

data class EditQuoteDraftUi(
    val quoteId: String,
    val quoteTitle: String,
    val customer: QuoteCustomerUi,
    val billingAddress: QuoteAddressUi,
    val shippingAddress: QuoteAddressUi,
    val paymentTerm: QuotePaymentTermUi,
    val deliveryDate: String,
    val products: List<QuoteCreationProductUi>,
    val dealRegistration: Boolean
)
