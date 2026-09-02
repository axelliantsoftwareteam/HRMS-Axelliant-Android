package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.quotes.data.remote.dto.AccountDdlDto
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationDeliveryScheduleDto
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationLineItemDto
import com.axelliant.hris.features.quotes.data.remote.dto.AddEditQuotationRequest
import com.axelliant.hris.features.quotes.data.remote.dto.CustomerAddressDto
import com.axelliant.hris.features.quotes.data.remote.dto.CustomerQuotationDetailsDto
import com.axelliant.hris.features.quotes.data.remote.dto.PaymentTermDdlDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteAddressDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuoteLineItemDto
import com.axelliant.hris.features.quotes.data.remote.dto.QuotePreviewResponse
import com.axelliant.hris.features.quotes.domain.model.CreateDraftQuoteRequest
import com.axelliant.hris.features.quotes.domain.model.EditQuoteDraftUi
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.quotes.domain.model.QuoteProductDeliveryScheduleUi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object QuoteCreationMapper {

    fun mapAccount(account: AccountDdlDto): QuoteCustomerUi? {
        val accountId = account.accountId?.trim().orEmpty()
        val name = account.name?.trim().orEmpty()
        if (accountId.isBlank() || name.isBlank()) return null

        return QuoteCustomerUi(
            id = accountId,
            name = name,
            email = account.customerEmail.orEmpty(),
            customerFirstName = account.customerFirstName.orEmpty(),
            isPriceProfileIdExist = account.isPriceProfileIdExist == true,
            catalog = account.catalog.orEmpty(),
            typeOfCustomer = account.typeOfCustomer.orEmpty(),
            accountExecutive = account.ae.orEmpty()
        )
    }

    fun applyCustomerDetails(
        customer: QuoteCustomerUi,
        details: CustomerQuotationDetailsDto
    ): QuoteCustomerUi {
        val billing = details.billingAddress.orEmpty().mapNotNull { mapBillingAddress(it) }
        val shipping = details.shippingAddress.orEmpty().mapNotNull { mapShippingAddress(it) }

        return customer.copy(
            priceProfileId = details.priceProfileId.orEmpty(),
            priceProfile = details.priceProfileName.orEmpty(),
            creditHoldEnabled = details.creditStatus == true,
            remainingCredit = CurrencyFormatter.format(details.currentBalance),
            billingAddresses = billing,
            shippingAddresses = shipping
        )
    }

    fun mapPaymentTerm(term: PaymentTermDdlDto): QuotePaymentTermUi? {
        val id = term.id?.trim().orEmpty()
        val name = term.name?.trim().orEmpty()
        if (id.isBlank() || name.isBlank()) return null
        return QuotePaymentTermUi(
            id = id,
            name = name,
            description = term.description.orEmpty(),
            paymentTermType = term.paymentTermType.orEmpty(),
            status = term.status.orEmpty()
        )
    }

    fun toAddEditRequest(
        quoteTitle: String,
        draft: CreateDraftQuoteRequest
    ): AddEditQuotationRequest {
        val parentDeliveryDate = formatApiDate(draft.deliveryDate)
        val subTotal = draft.products.sumOf { product ->
            product.effectiveBasePrice * product.quantity
        }
        return AddEditQuotationRequest(
            id = draft.quoteId,
            quoteName = quoteTitle,
            accountId = draft.customer.id,
            priceProfileId = draft.customer.priceProfileId,
            billingAddressId = draft.billingAddress.id,
            shippingAddressId = draft.shippingAddress.id,
            paymentTermId = draft.paymentTerm.id,
            isCreditHold = draft.customer.creditHoldEnabled == true,
            subTotal = subTotal,
            grandTotal = subTotal,
            isDealRegistration = draft.dealRegistration,
            isWarehouse = draft.isWarehouse,
            lineItems = draft.products.map { product ->
                toLineItem(
                    product = product,
                    draft = draft,
                    parentDeliveryDate = parentDeliveryDate
                )
            },
            parentDeliveryDate = parentDeliveryDate.takeIf { it.isNotBlank() }
        )
    }

    private fun toLineItem(
        product: QuoteCreationProductUi,
        draft: CreateDraftQuoteRequest,
        parentDeliveryDate: String
    ): AddEditQuotationLineItemDto {
        val unitPrice = product.effectiveBasePrice
        return AddEditQuotationLineItemDto(
            id = product.lineItemId,
            productId = product.id,
            basePrice = unitPrice,
            unitPrice = unitPrice,
            quantity = product.quantity,
            lineTotal = unitPrice * product.quantity,
            customDeliveryDates = product.deliverySchedules
                .map { schedule ->
                    AddEditQuotationDeliveryScheduleDto(
                        quantity = schedule.quantity,
                        deliveryDate = formatApiDate(schedule.estimatedDeliveryDate)
                            .ifBlank { parentDeliveryDate },
                        shippingAddressId = schedule.shippingAddress.id.ifBlank {
                            draft.shippingAddress.id
                        }
                    )
                }
                .ifEmpty {
                    listOf(
                        AddEditQuotationDeliveryScheduleDto(
                            quantity = product.quantity,
                            deliveryDate = parentDeliveryDate,
                            shippingAddressId = draft.shippingAddress.id
                        )
                    )
                }
        )
    }

    fun toEditDraft(response: QuotePreviewResponse, fallbackQuoteId: String): EditQuoteDraftUi? {
        val quoteId = response.id
            ?: response.quotationId
            ?: fallbackQuoteId
        val accountId = response.accountId?.trim().orEmpty()
        val customerName = resolveCustomerName(response, accountId)
        val billingAddress = response.billingAddress.toQuoteAddressUi(
            fallbackId = response.billingAddressId,
            isBilling = true
        ) ?: quoteAddressFromId(response.billingAddressId)
        val shippingAddress = response.shippingAddress.toQuoteAddressUi(
            fallbackId = response.shippingAddressId,
            isBilling = false
        ) ?: resolveShippingAddress(response)
            ?: quoteAddressFromId(response.shippingAddressId)
        if (billingAddress == null || shippingAddress == null) {
            return null
        }
        val paymentTermId = response.paymentTermId?.trim().orEmpty()
        if (quoteId.isBlank() || accountId.isBlank() || paymentTermId.isBlank()) {
            return null
        }

        val deliveryDate = formatUiDate(response.parentDeliveryDate ?: response.deliveryDate.orEmpty())
        val paymentTerm = QuotePaymentTermUi(
            id = paymentTermId,
            name = response.paymentTerm.orEmpty().ifBlank { "Payment Term" }
        )
        val customer = QuoteCustomerUi(
            id = accountId,
            name = customerName,
            email = response.customerEmail.orEmpty(),
            creditHoldEnabled = response.isCreditHold ?: response.creditStatus,
            remainingCredit = CurrencyFormatter.format(response.currentBalance),
            accountExecutive = response.ae.orEmpty(),
            priceProfile = response.priceProfileName.orEmpty(),
            priceProfileId = response.priceProfileId.orEmpty(),
            billingAddresses = listOf(billingAddress),
            shippingAddresses = listOf(shippingAddress)
        )

        return EditQuoteDraftUi(
            quoteId = quoteId,
            quoteTitle = response.quoteName
                ?: response.quoteSerialNo
                ?: fallbackQuoteId,
            customer = customer,
            billingAddress = billingAddress,
            shippingAddress = shippingAddress,
            paymentTerm = paymentTerm,
            deliveryDate = deliveryDate,
            products = response.lineItems.orEmpty().mapNotNull { item ->
                item.toQuoteCreationProductUi(
                    defaultShippingAddress = shippingAddress,
                    defaultDeliveryDate = deliveryDate
                )
            },
            dealRegistration = response.isDealRegistration ?: (response.dealRegistration == true)
        )
    }

    private fun resolveCustomerName(response: QuotePreviewResponse, accountId: String): String {
        return sequenceOf(
            response.accountName,
            response.acountName,
            response.accountSerialNo,
            response.customerEmail,
            response.quoteName
        )
            .map { it?.trim().orEmpty() }
            .firstOrNull { it.isNotBlank() }
            ?: accountId
    }

    private fun resolveShippingAddress(response: QuotePreviewResponse): QuoteAddressUi? {
        return response.lineItems.orEmpty()
            .asSequence()
            .flatMap { it.customDeliveryDates.orEmpty().asSequence() }
            .mapNotNull { schedule ->
                schedule.shippingAddress.toQuoteAddressUi(
                    fallbackId = schedule.shippingAddressId,
                    isBilling = false
                )
            }
            .firstOrNull()
    }

    private fun quoteAddressFromId(addressId: String?): QuoteAddressUi? {
        val resolvedId = addressId?.trim().orEmpty()
        if (resolvedId.isBlank()) return null
        return QuoteAddressUi(
            id = resolvedId,
            address = "",
            country = "",
            state = "",
            city = "",
            zipCode = ""
        )
    }

    private fun QuoteLineItemDto.toQuoteCreationProductUi(
        defaultShippingAddress: QuoteAddressUi,
        defaultDeliveryDate: String
    ): QuoteCreationProductUi? {
        val productId = productId?.trim().orEmpty().ifBlank { product?.id?.trim().orEmpty() }
        if (productId.isBlank()) return null
        val quantity = quantity ?: 1
        val unitPrice = unitPrice ?: basePrice ?: 0.0
        val resolvedBasePrice = basePrice ?: unitPrice
        val resolvedName = productName.orEmpty().ifBlank { product?.name.orEmpty() }
        val resolvedSku = sku?.trim().orEmpty()
            .ifBlank { axePartNo?.trim().orEmpty() }
            .ifBlank { product?.axePartNo?.trim().orEmpty() }
            .ifBlank { product?.mfgPartNo?.trim().orEmpty() }
        val schedules = customDeliveryDates.orEmpty().map { schedule ->
            val scheduleAddress = schedule.shippingAddress.toQuoteAddressUi(
                fallbackId = schedule.shippingAddressId,
                isBilling = false
            ) ?: defaultShippingAddress
            QuoteProductDeliveryScheduleUi(
                id = UUID.randomUUID().toString(),
                shippingAddress = scheduleAddress,
                quantity = schedule.quantity ?: quantity,
                estimatedDeliveryDate = formatUiDate(schedule.deliveryDate.orEmpty())
                    .ifBlank { defaultDeliveryDate },
                isDefaultAddress = scheduleAddress.id == defaultShippingAddress.id
            )
        }

        return QuoteCreationProductUi(
            id = productId,
            lineItemId = id,
            name = resolvedName,
            sku = resolvedSku,
            category = "",
            thumbnailLabel = resolvedName.take(1).uppercase(Locale.US).ifBlank { "P" },
            brandThumbnail = false,
            unitPrice = unitPrice,
            basePrice = resolvedBasePrice,
            quantity = quantity,
            deliverySchedules = schedules.ifEmpty {
                listOf(
                    QuoteProductDeliveryScheduleUi(
                        id = UUID.randomUUID().toString(),
                        shippingAddress = defaultShippingAddress,
                        quantity = quantity,
                        estimatedDeliveryDate = defaultDeliveryDate,
                        isDefaultAddress = true
                    )
                )
            }
        )
    }

    private fun QuoteAddressDto?.toQuoteAddressUi(
        fallbackId: String?,
        isBilling: Boolean
    ): QuoteAddressUi? {
        val address = this ?: return null
        val resolvedId = fallbackId
            ?: if (isBilling) address.billingAddressId else address.shippingAddressId
            ?: address.addressId
            ?: address.id
        return mapAddress(
            id = resolvedId,
            street = address.address,
            country = address.country,
            state = address.stateOrProvince,
            city = address.city,
            zipCode = address.zip ?: address.zipCode
        )
    }

    private fun formatApiDate(date: String): String {
        if (date.isBlank()) return ""
        val inputFormats = listOf("yyyy-MM-dd", "MM-dd-yyyy", "MM/dd/yyyy")
        for (pattern in inputFormats) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US)
                    .apply { isLenient = false }
                    .parse(date) ?: continue
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed)
            } catch (_: Exception) {
                continue
            }
        }
        return date
    }

    private fun mapBillingAddress(address: CustomerAddressDto): QuoteAddressUi? {
        val id = address.billingAddressId
            ?: address.addressId
            ?: address.id
        return mapAddress(id, address)
    }

    private fun mapShippingAddress(address: CustomerAddressDto): QuoteAddressUi? {
        val id = address.shippingAddressId
            ?: address.addressId
            ?: address.id
        return mapAddress(id, address)
    }

    private fun mapAddress(id: String?, address: CustomerAddressDto): QuoteAddressUi? {
        return mapAddress(
            id = id,
            street = address.address,
            country = address.country,
            state = address.stateOrProvince ?: address.state,
            city = address.city,
            zipCode = address.zip ?: address.zipCode
        )
    }

    private fun mapAddress(
        id: String?,
        street: String?,
        country: String?,
        state: String?,
        city: String?,
        zipCode: String?
    ): QuoteAddressUi? {
        val resolvedId = id?.trim().orEmpty()
        val resolvedStreet = street?.trim().orEmpty()
        if (resolvedId.isBlank() || resolvedStreet.isBlank()) return null

        return QuoteAddressUi(
            id = resolvedId,
            address = resolvedStreet,
            country = country.orEmpty(),
            state = state.orEmpty(),
            city = city.orEmpty(),
            zipCode = zipCode.orEmpty()
        )
    }

    private fun formatUiDate(date: String): String {
        if (date.isBlank()) return ""
        val inputFormats = listOf("MM-dd-yyyy", "MM/dd/yyyy", "yyyy-MM-dd")
        for (pattern in inputFormats) {
            try {
                val parsed = SimpleDateFormat(pattern, Locale.US)
                    .apply { isLenient = false }
                    .parse(date) ?: continue
                return SimpleDateFormat("MM-dd-yyyy", Locale.US).format(parsed)
            } catch (_: Exception) {
                continue
            }
        }
        return date
    }
}
