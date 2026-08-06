package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteAddressDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuotePreviewResponse
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewProductUiModel
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object QuotePreviewMapper {
    private const val FALLBACK = "N/A"
    private val inputDateFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    private val outputDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)

    fun toUiModel(response: QuotePreviewResponse): QuotePreviewUiModel {
        return QuotePreviewUiModel(
            quoteTypeLabel = response.resolveQuoteTypeLabel(),
            quoteNumber = response.quoteSerialNo.orFallback(),
            statusLabel = response.approvalStatus.orFallback(),
            status = resolveStatus(response.approvalStatus),
            createdDate = formatDate(response.createdDate),
            createdBy = response.createdBy.orFallback(),
            email = response.createdByEmail.orFallback(),
            ae = response.ae.orFallback(),
            customerName = response.acountName.orFallback(),
            paymentTerms = response.paymentTerm.orFallback(),
            billingDetails = formatAddress(response.billingAddress),
            shippingDetails = formatAddress(response.shippingAddress),
            products = response.lineItems.orEmpty().map { item ->
                QuotePreviewProductUiModel(
                    sku = item.axePartNo.orFallback(),
                    quantity = item.quantity?.toString().orFallback(),
                    productName = item.productName.orFallback(),
                    unitPrice = CurrencyFormatter.format(item.unitPrice),
                    lineTotal = CurrencyFormatter.format(item.lineTotal)
                )
            },
            hasComments = !response.comments.isNullOrEmpty(),
            subtotal = CurrencyFormatter.format(response.subTotal),
            tax = CurrencyFormatter.format(response.taxAmount),
            shipping = CurrencyFormatter.format(response.shipping),
            grandTotal = CurrencyFormatter.format(response.grandTotal)
        )
    }

    private fun QuotePreviewResponse.resolveQuoteTypeLabel(): String {
        return quotationType?.takeIf { it.isNotBlank() }
            ?: quoteSource?.takeIf { it.isNotBlank() }
            ?: FALLBACK
    }

    private fun formatAddress(address: QuoteAddressDto?): String {
        if (address == null) return FALLBACK
        val parts = listOfNotNull(
            address.address?.trim()?.takeIf { it.isNotEmpty() },
            address.city?.trim()?.takeIf { it.isNotEmpty() },
            address.stateOrProvince?.trim()?.takeIf { it.isNotEmpty() },
            address.country?.trim()?.takeIf { it.isNotEmpty() }
        )
        return parts.joinToString(", ").ifBlank { FALLBACK }
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        inputDateFormats.forEach { pattern ->
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw)
            }.getOrNull()
            if (parsed != null) {
                return outputDateFormat.format(parsed)
            }
        }
        return raw
    }

    private fun resolveStatus(raw: String?): QuoteStatus? {
        if (raw.isNullOrBlank()) return null
        raw.trim().toIntOrNull()?.let { apiValue ->
            QuoteStatus.fromApiValue(apiValue)?.let { return it }
        }
        return when (raw.trim().lowercase()) {
            "approved" -> QuoteStatus.Approved
            "submitted" -> QuoteStatus.Submitted
            "rejected" -> QuoteStatus.Rejected
            "saveasdraft", "draft" -> QuoteStatus.Draft
            "awaitingapproval", "awaiting approval" -> QuoteStatus.AwaitingApproval
            "cancelled", "canceled" -> QuoteStatus.Cancelled
            else -> null
        }
    }

    private fun String?.orFallback(): String = this?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK
}
