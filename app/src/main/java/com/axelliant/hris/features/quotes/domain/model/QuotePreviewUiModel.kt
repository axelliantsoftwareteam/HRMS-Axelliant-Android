package com.axelliant.hris.features.quotes.domain.model

data class QuotePreviewUiModel(
    val quoteTypeLabel: String,
    val quoteNumber: String,
    val statusLabel: String,
    val status: QuoteStatus?,
    val createdDate: String,
    val createdBy: String,
    val email: String,
    val ae: String,
    val customerName: String,
    val paymentTerms: String,
    val billingDetails: String,
    val shippingDetails: String,
    val products: List<QuotePreviewProductUiModel>,
    val hasComments: Boolean,
    val subtotal: String,
    val tax: String,
    val shipping: String,
    val grandTotal: String
)

data class QuotePreviewProductUiModel(
    val sku: String,
    val quantity: String,
    val productName: String,
    val unitPrice: String,
    val lineTotal: String
)
