package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.features.quotes.data.remote.dto.QuoteAddressDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteReportFooterAddressDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteReportLineItemDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteReportResponse
import com.axelliant.hris.features.quotes.domain.model.QuoteReportFieldUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteReportLineItemUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteReportUiModel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

object QuoteReportMapper {
    private const val EMPTY_VALUE = "-"

    fun toUiModel(response: QuoteReportResponse): QuoteReportUiModel {
        return QuoteReportUiModel(
            quoteId = response.id.orEmpty(),
            quoteNumber = response.quoteSerialId.valueOrEmptyFallback(response.quoteTitle),
            createdDate = response.quoteCreatedOn.formatCreatedDate(response.quoteCreatedDate),
            customerTitle = buildCustomerTitle(response),
            companyAddress = response.axelliantAddress.valueOrDash(),
            companyPhone = response.axelliantPhone.valueOrDash(),
            billingAddress = response.billingAddress.formatAddress(),
            shippingAddress = response.shippingAddress.formatAddress(),
            quoteInformation = buildQuoteInformation(response),
            contactInformation = buildContactInformation(response),
            lineItems = response.lineItems.orEmpty().mapIndexed { index, item ->
                item.toUiModel(index + 1)
            },
            totals = buildTotals(response),
            grandTotal = response.grandTotal.formatCurrency(),
            acceptance = buildAcceptance(response),
            footerText = buildFooter(response)
        )
    }

    private fun buildCustomerTitle(response: QuoteReportResponse): String {
        val customer = response.customerName.valueOrEmptyFallback(response.serviceType)
        val serial = response.customerSerialId.orEmpty().trim()
        return if (serial.isBlank()) customer else "$customer $serial"
    }

    private fun buildQuoteInformation(response: QuoteReportResponse): List<QuoteReportFieldUiModel> {
        return listOf(
            QuoteReportFieldUiModel("Quote Title", response.quoteTitle.valueOrEmptyFallback(response.quoteSerialId)),
            QuoteReportFieldUiModel("Payment", response.paymentTerm.valueOrDash()),
            QuoteReportFieldUiModel("Shipping", response.shippingService.valueOrDash()),
            QuoteReportFieldUiModel("Weight", response.weight.formatNumber()),
            QuoteReportFieldUiModel("Signature Options", response.signatureOptions.valueOrDash())
        )
    }

    private fun buildContactInformation(response: QuoteReportResponse): List<QuoteReportFieldUiModel> {
        return listOf(
            QuoteReportFieldUiModel("Created By", response.createdBy.valueOrDash()),
            QuoteReportFieldUiModel("Phone", response.phone.valueOrDash()),
            QuoteReportFieldUiModel("Email", response.email.valueOrDash()),
            QuoteReportFieldUiModel("AE", response.ae.valueOrDash())
        )
    }

    private fun buildTotals(response: QuoteReportResponse): List<QuoteReportFieldUiModel> {
        return listOf(
            QuoteReportFieldUiModel("Hardware/Software Subtotal", response.subTotal.formatCurrency()),
            QuoteReportFieldUiModel("Recycle Fee", response.recycleFee.formatCurrency()),
            QuoteReportFieldUiModel("Sales Tax", response.taxAmount.formatCurrency()),
            QuoteReportFieldUiModel("Shipping", response.shipping.formatCurrency())
        )
    }

    private fun buildAcceptance(response: QuoteReportResponse): List<QuoteReportFieldUiModel> {
        return listOf(
            QuoteReportFieldUiModel("Name", response.customerName.valueOrDash()),
            QuoteReportFieldUiModel("Email", response.customerEmail.valueOrDash()),
            QuoteReportFieldUiModel("Phone", response.customerPhone.valueOrDash()),
            QuoteReportFieldUiModel("PO #", response.po.valueOrDash()),
            QuoteReportFieldUiModel("Signature", response.signature.valueOrEmptyFallback(response.signatureImage)),
            QuoteReportFieldUiModel("Date", response.customerDate.valueOrDash())
        )
    }

    private fun buildFooter(response: QuoteReportResponse): String {
        val address = response.footerAddress.formatAddress()
        return listOf(response.tagLine.valueOrDash(), address)
            .filter { it != EMPTY_VALUE }
            .joinToString(separator = "\n")
            .ifBlank { EMPTY_VALUE }
    }

    private fun QuoteReportLineItemDto.toUiModel(lineNumber: Int): QuoteReportLineItemUiModel {
        return QuoteReportLineItemUiModel(
            lineNumber = "Line #$lineNumber",
            manufacturer = manufacturer.valueOrDash(),
            mfgPartNo = mfgPartNo.valueOrDash(),
            axePartNo = axePartNo.valueOrDash(),
            name = name.valueOrDash(),
            description = description.valueOrDash(),
            quantity = quantity?.toString() ?: EMPTY_VALUE,
            unitPrice = unitPrice.formatCurrency(),
            lineTotal = lineTotal.formatCurrency(),
            duration = if (showDuration == true) duration.valueOrDash() else EMPTY_VALUE
        )
    }

    private fun Long?.formatCreatedDate(fallback: String?): String {
        val timestamp = this ?: return fallback.valueOrDash()
        return runCatching {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(Date(timestamp * 1000L))
        }.getOrDefault(fallback.valueOrDash())
    }

    private fun Double?.formatCurrency(): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(this ?: 0.0)
    }

    private fun Double?.formatNumber(): String {
        val value = this ?: return EMPTY_VALUE
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private fun QuoteAddressDto?.formatAddress(): String {
        if (this == null) return EMPTY_VALUE
        return listOf(address, city, stateOrProvince, country, zip ?: zipCode)
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(separator = ", ")
            .ifBlank { EMPTY_VALUE }
    }

    private fun QuoteReportFooterAddressDto?.formatAddress(): String {
        if (this == null) return EMPTY_VALUE
        return listOf(address, city, state, zip, country)
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(separator = ", ")
            .ifBlank { EMPTY_VALUE }
    }

    private fun String?.valueOrDash(): String {
        return orEmpty().trim().ifBlank { EMPTY_VALUE }
    }

    private fun String?.valueOrEmptyFallback(fallback: String?): String {
        return orEmpty().trim().ifBlank { fallback.valueOrDash() }
    }
}
