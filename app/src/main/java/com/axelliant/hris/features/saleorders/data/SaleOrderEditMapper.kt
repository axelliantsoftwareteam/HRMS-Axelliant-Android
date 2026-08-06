package com.axelliant.hris.features.saleorders.data

import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.saleorders.domain.model.EditSaleOrderDraftUi
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderDetailModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderProductLine
import java.util.Locale

object SaleOrderEditMapper {

    fun toEditDraft(detail: SaleOrderDetailModel): EditSaleOrderDraftUi {
        val billingAddress = addressFromDisplay(detail.billingAddress, idPrefix = "${detail.id}-billing")
        val shippingAddress = addressFromDisplay(detail.shippingAddress, idPrefix = "${detail.id}-shipping")
        return EditSaleOrderDraftUi(
            saleOrderId = detail.id,
            orderNumber = detail.orderNumber,
            customer = QuoteCustomerUi(
                id = "",
                name = detail.customerName
            ),
            billingAddress = billingAddress,
            shippingAddress = shippingAddress,
            paymentTerm = QuotePaymentTermUi(
                id = "",
                name = detail.paymentTerms
            ),
            deliveryDate = detail.deliveryDate,
            shippingMethodId = "ground",
            products = detail.products.map(::mapProduct)
        )
    }

    private fun mapProduct(line: SaleOrderProductLine): QuoteCreationProductUi {
        val unitPrice = parseCurrency(line.unitPrice)
        return QuoteCreationProductUi(
            id = line.id,
            name = line.name,
            sku = line.sku,
            category = "",
            thumbnailLabel = line.name.take(1).uppercase(Locale.US).ifBlank { "P" },
            brandThumbnail = false,
            unitPrice = unitPrice,
            basePrice = unitPrice,
            quantity = line.quantity.coerceAtLeast(1)
        )
    }

    private fun addressFromDisplay(display: String, idPrefix: String): QuoteAddressUi {
        val trimmed = display.trim()
        return QuoteAddressUi(
            id = idPrefix,
            address = trimmed,
            country = "",
            state = "",
            city = "",
            zipCode = ""
        )
    }

    private fun parseCurrency(value: String): Double {
        val cleaned = value.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
