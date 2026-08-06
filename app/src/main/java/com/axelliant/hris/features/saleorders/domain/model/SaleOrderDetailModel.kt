package com.axelliant.hris.features.saleorders.domain.model

data class SaleOrderProductLine(
    val id: String,
    val name: String,
    val sku: String,
    val unitPrice: String,
    val quantity: Int,
    val lineTotal: String
)

data class SaleOrderDetailModel(
    val id: String,
    val orderNumber: String,
    val createdDate: String,
    val status: SaleOrderStatus,
    val assigneeName: String,
    val customerName: String,
    val paymentTerms: String,
    val billingAddress: String,
    val shippingAddress: String,
    val products: List<SaleOrderProductLine>,
    val deliveryDate: String,
    val validity: String,
    val expiresOn: String,
    val dealRegistrationId: String,
    val dealRegistrationStatus: String,
    val dealRegistrationDocument: String,
    val subtotal: String,
    val tax: String,
    val shipping: String,
    val grandTotal: String
)
