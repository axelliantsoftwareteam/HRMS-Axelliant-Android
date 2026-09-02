package com.axelliant.hris.features.quotes.domain.model

data class QuoteReportUiModel(
    val quoteId: String,
    val quoteNumber: String,
    val createdDate: String,
    val customerTitle: String,
    val companyAddress: String,
    val companyPhone: String,
    val billingAddress: String,
    val shippingAddress: String,
    val quoteInformation: List<QuoteReportFieldUiModel>,
    val contactInformation: List<QuoteReportFieldUiModel>,
    val lineItems: List<QuoteReportLineItemUiModel>,
    val totals: List<QuoteReportFieldUiModel>,
    val grandTotal: String,
    val acceptance: List<QuoteReportFieldUiModel>,
    val footerText: String
)

data class QuoteReportFieldUiModel(
    val label: String,
    val value: String
)

data class QuoteReportLineItemUiModel(
    val lineNumber: String,
    val manufacturer: String,
    val mfgPartNo: String,
    val axePartNo: String,
    val name: String,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val lineTotal: String,
    val duration: String
)
