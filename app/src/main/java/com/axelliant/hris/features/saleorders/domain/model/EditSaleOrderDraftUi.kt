package com.axelliant.hris.features.saleorders.domain.model

import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi

data class EditSaleOrderDraftUi(
    val saleOrderId: String,
    val orderNumber: String,
    val customer: QuoteCustomerUi,
    val billingAddress: QuoteAddressUi,
    val shippingAddress: QuoteAddressUi,
    val paymentTerm: QuotePaymentTermUi,
    val deliveryDate: String,
    val shippingMethodId: String,
    val status: SaleOrderStatus = SaleOrderStatus.DRAFT,
    val products: List<QuoteCreationProductUi>
)

data class SaveSaleOrderRequest(
    val saleOrderId: String?,
    val orderNumber: String,
    val customer: QuoteCustomerUi,
    val billingAddress: QuoteAddressUi,
    val shippingAddress: QuoteAddressUi,
    val paymentTerm: QuotePaymentTermUi,
    val deliveryDate: String,
    val shippingType: Int,
    val products: List<QuoteCreationProductUi>
)

data class SaveSaleOrderResult(
    val message: String
)

data class SubmitSaleOrderResult(
    val message: String
)
