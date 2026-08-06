package com.axelliant.hris.features.quotes.domain.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object QuoteProductScheduleHelper {

    fun ensureSchedules(
        product: QuoteCreationProductUi,
        quoteShippingAddress: QuoteAddressUi?,
        quoteDeliveryDate: String = ""
    ): QuoteCreationProductUi {
        val withBasePrice = product.copy(basePrice = product.basePrice ?: product.unitPrice)
        if (quoteShippingAddress == null) return withBasePrice

        val scheduleDate = formatDeliveryDate(quoteDeliveryDate)
        if (withBasePrice.deliverySchedules.isNotEmpty()) {
            return withBasePrice.copy(
                deliverySchedules = withBasePrice.deliverySchedules.mapIndexed { index, schedule ->
                    if (index == 0) {
                        schedule.copy(
                            shippingAddress = quoteShippingAddress,
                            isDefaultAddress = true,
                            estimatedDeliveryDate = schedule.estimatedDeliveryDate
                                .ifBlank { scheduleDate }
                        )
                    } else {
                        schedule
                    }
                }
            )
        }

        return withBasePrice.copy(
            deliverySchedules = listOf(
                QuoteProductDeliveryScheduleUi(
                    id = UUID.randomUUID().toString(),
                    shippingAddress = quoteShippingAddress,
                    quantity = withBasePrice.quantity,
                    estimatedDeliveryDate = scheduleDate,
                    isDefaultAddress = true
                )
            )
        )
    }

    fun formatDeliveryDate(quoteDeliveryDate: String): String {
        if (quoteDeliveryDate.isBlank()) return ""
        val inputFormats = listOf("MM-dd-yyyy", "MM/dd/yyyy")
        for (pattern in inputFormats) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(quoteDeliveryDate) ?: continue
                return SimpleDateFormat("MM/dd/yyyy", Locale.US).format(parsed)
            } catch (_: Exception) {
                continue
            }
        }
        return quoteDeliveryDate
    }
}
