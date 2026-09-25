package com.axelliant.hris.features.saleorders.data

import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import com.axelliant.hris.features.quotes.domain.model.QuotePaymentTermUi
import com.axelliant.hris.features.saleorders.data.remote.dto.AddEditSaleOrderLineDto
import com.axelliant.hris.features.saleorders.data.remote.dto.AddEditSaleOrderRequest
import com.axelliant.hris.features.saleorders.data.remote.dto.GetSaleOrdersResponse
import com.axelliant.hris.features.saleorders.data.remote.dto.SaleOrderAddressDto
import com.axelliant.hris.features.saleorders.data.remote.dto.SaleOrderDetailLineDto
import com.axelliant.hris.features.saleorders.data.remote.dto.SaleOrderListItemDto
import com.axelliant.hris.features.saleorders.data.remote.dto.SaleOrderSingleDto
import com.axelliant.hris.features.saleorders.domain.model.EditSaleOrderDraftUi
import com.axelliant.hris.features.saleorders.domain.model.SaveSaleOrderRequest
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderDetailModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderPageResult
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderProductLine
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatus
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatusFilterType
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderUtilization
import java.text.SimpleDateFormat
import java.util.Locale

object SaleOrderApiMapper {
    private const val FALLBACK = "N/A"
    private const val API_STATUS_PENDING = 1
    private const val API_STATUS_RELEASED = 2
    private const val API_STATUS_CANCELED = 3
    private const val API_STATUS_APPROVED = 4
    private const val API_STATUS_DRAFT = 5

    fun mapPage(response: GetSaleOrdersResponse): SaleOrderPageResult {
        val orders = response.dataList.orEmpty().map(::toListModel)
        return SaleOrderPageResult(
            orders = orders,
            totalCount = response.totalCount ?: orders.size,
            statusCounts = buildStatusCounts(orders)
        )
    }

    fun toDetail(dto: SaleOrderSingleDto): SaleOrderDetailModel {
        val lines = dto.salesOrderDetail.orEmpty()
        val productShipping = lines.sumOf { it.shipping ?: 0.0 }
        return SaleOrderDetailModel(
            id = dto.id.orEmpty(),
            orderNumber = dto.soId.orFallback(),
            createdDate = dto.createdDate.orFallback(),
            status = dto.status.toStatus(),
            assigneeName = dto.createdBy.orFallback(),
            customerName = dto.customerName.orFallback(),
            paymentTerms = dto.paymentTermName.orFallback(),
            billingAddress = dto.billingAddress.toDisplayAddress(),
            shippingAddress = dto.shippingAddress.toDisplayAddress(),
            products = lines.map(::toProductLine),
            deliveryDate = formatDateForUi(dto.deliveryDate),
            validity = dto.validity.orFallback(),
            expiresOn = dto.expiryDate.orFallback(),
            dealRegistrationId = dto.dealRegistrationId.orFallback(),
            dealRegistrationStatus = if (dto.isDealRegistration == true) "TRUE" else "FALSE",
            dealRegistrationDocument = dto.isDealRegistrationId.orFallback(),
            subtotal = CurrencyFormatter.format(dto.subTotal),
            tax = CurrencyFormatter.format(dto.taxAmount),
            shipping = CurrencyFormatter.format(productShipping),
            grandTotal = CurrencyFormatter.format(dto.grandTotal)
        )
    }

    fun toEditDraft(dto: SaleOrderSingleDto): EditSaleOrderDraftUi {
        val billingAddress = dto.billingAddress.toQuoteAddress(
            fallbackId = dto.billingAddressId.orEmpty()
        )
        val shippingAddress = dto.shippingAddress.toQuoteAddress(
            fallbackId = dto.shippingAddressId.orEmpty()
        )
        return EditSaleOrderDraftUi(
            saleOrderId = dto.id.orEmpty(),
            orderNumber = dto.soId.orEmpty(),
            customer = QuoteCustomerUi(
                id = dto.accountId.orEmpty(),
                name = dto.customerName.orFallback(),
                accountExecutive = dto.createdBy.orEmpty(),
                priceProfileId = dto.priceProfileId.orEmpty(),
                billingAddresses = listOf(billingAddress),
                shippingAddresses = listOf(shippingAddress)
            ),
            billingAddress = billingAddress,
            shippingAddress = shippingAddress,
            paymentTerm = QuotePaymentTermUi(
                id = dto.paymentTermId.orEmpty(),
                name = dto.paymentTermName.orFallback()
            ),
            deliveryDate = formatDateForUi(dto.deliveryDate),
            shippingMethodId = dto.shippingType?.toString().orEmpty().ifBlank { "1" },
            status = dto.status.toStatus(),
            products = dto.salesOrderDetail.orEmpty().map(::toCreationProduct)
        )
    }

    fun toAddEditRequest(request: SaveSaleOrderRequest): AddEditSaleOrderRequest {
        val deliveryDate = formatDateForApi(request.deliveryDate)
        return AddEditSaleOrderRequest(
            id = request.saleOrderId,
            shippingType = request.shippingType,
            accountId = request.customer.id,
            priceProfileId = request.customer.priceProfileId,
            billingAddressId = request.billingAddress.id,
            shippingAddressId = request.shippingAddress.id,
            soId = request.orderNumber,
            paymentTermId = request.paymentTerm.id,
            subTotal = request.products.sumOf { it.lineTotal },
            grandTotal = request.products.sumOf { it.lineTotal },
            salesOrderDetail = request.products.map { product ->
                AddEditSaleOrderLineDto(
                    id = product.lineItemId,
                    productId = product.id.takeIf { it.isNotBlank() },
                    salesOrderId = request.saleOrderId,
                    deliveryDate = deliveryDate,
                    basePrice = product.effectiveBasePrice,
                    paymentTermId = request.paymentTerm.id,
                    shippingAddressId = request.shippingAddress.id,
                    unitPrice = product.unitPrice,
                    quantity = product.quantity.coerceAtLeast(1),
                    totalPrice = product.lineTotal
                )
            },
            status = API_STATUS_DRAFT,
            deliveryDate = deliveryDate
        )
    }

    private fun toListModel(dto: SaleOrderListItemDto): SaleOrderModel {
        return SaleOrderModel(
            id = dto.id.orEmpty(),
            orderNumber = dto.soId.orFallback(),
            customerName = dto.accountName.orFallback(),
            status = dto.status.toStatus(),
            utilization = dto.utilizingStatus.toUtilization(),
            grandTotal = CurrencyFormatter.format(dto.grandTotal),
            deliveryDate = formatDateForUi(dto.deliveryDate)
        )
    }

    private fun toProductLine(dto: SaleOrderDetailLineDto): SaleOrderProductLine {
        return SaleOrderProductLine(
            id = dto.id.orEmpty(),
            name = dto.productName?.takeIf { it.isNotBlank() }
                ?: dto.description.orFallback(),
            sku = dto.axePartNo.orFallback(),
            unitPrice = CurrencyFormatter.format(dto.unitPrice),
            quantity = dto.quantity ?: 0,
            lineTotal = CurrencyFormatter.format(dto.totalPrice)
        )
    }

    private fun toCreationProduct(dto: SaleOrderDetailLineDto): QuoteCreationProductUi {
        val name = dto.productName?.takeIf { it.isNotBlank() }
            ?: dto.description.orFallback()
        val unitPrice = dto.unitPrice ?: dto.basePrice ?: 0.0
        return QuoteCreationProductUi(
            id = dto.productId.orEmpty(),
            lineItemId = dto.id,
            name = name,
            sku = dto.axePartNo.orFallback(),
            category = "",
            thumbnailLabel = name.take(1).uppercase(Locale.US).ifBlank { "P" },
            brandThumbnail = false,
            unitPrice = unitPrice,
            basePrice = dto.basePrice ?: unitPrice,
            quantity = (dto.quantity ?: 1).coerceAtLeast(1)
        )
    }

    private fun buildStatusCounts(
        orders: List<SaleOrderModel>
    ): Map<SaleOrderStatusFilterType, Int> {
        return mapOf(
            SaleOrderStatusFilterType.ALL to orders.size,
            SaleOrderStatusFilterType.RELEASED to orders.count { it.status == SaleOrderStatus.RELEASED },
            SaleOrderStatusFilterType.PENDING to orders.count { it.status == SaleOrderStatus.PENDING },
            SaleOrderStatusFilterType.CANCELED to orders.count { it.status == SaleOrderStatus.CANCELED }
        )
    }

    private fun Int?.toStatus(): SaleOrderStatus {
        return when (this) {
            API_STATUS_DRAFT -> SaleOrderStatus.DRAFT
            API_STATUS_PENDING -> SaleOrderStatus.PENDING
            API_STATUS_RELEASED -> SaleOrderStatus.RELEASED
            API_STATUS_CANCELED -> SaleOrderStatus.CANCELED
            API_STATUS_APPROVED -> SaleOrderStatus.APPROVED
            else -> SaleOrderStatus.PENDING
        }
    }

    private fun String?.toUtilization(): SaleOrderUtilization {
        val value = this.orEmpty()
        return when {
            value.equals("Partially Utilized", ignoreCase = true) -> SaleOrderUtilization.PARTIALLY_UTILIZED
            value.equals("Fully Utilized", ignoreCase = true) -> SaleOrderUtilization.FULLY_UTILIZED
            else -> SaleOrderUtilization.PENDING
        }
    }

    private fun SaleOrderAddressDto?.toQuoteAddress(fallbackId: String): QuoteAddressUi {
        return QuoteAddressUi(
            id = this?.id.orEmpty().ifBlank { fallbackId },
            address = this?.address.orEmpty(),
            country = this?.country.orEmpty(),
            state = this?.stateOrProvince ?: this?.state.orEmpty(),
            city = this?.city.orEmpty(),
            zipCode = this?.zip ?: this?.zipCode.orEmpty()
        )
    }

    private fun SaleOrderAddressDto?.toDisplayAddress(): String {
        return this.toQuoteAddress(fallbackId = "").displayTextWithLocation
            .ifBlank { FALLBACK }
    }

    private fun formatDateForUi(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        parseDate(raw)?.let {
            return SimpleDateFormat("MM-dd-yyyy", Locale.US).format(it)
        }
        return raw
    }

    private fun formatDateForApi(raw: String): String {
        if (raw.isBlank()) return raw
        parseDate(raw)?.let {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it)
        }
        return raw
    }

    private fun parseDate(raw: String): java.util.Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
            "MM-dd-yyyy",
            "MM/dd/yyyy",
            "MMM dd, yyyy",
            "MMM dd, yyyy, hh:mm:ss a"
        )
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(raw)
            }.getOrNull()
        }
    }

    private fun String?.orFallback(): String = this?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK
}
