package com.axelliant.hris.features.purchaseorders.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderResponseDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.CompanyAddressDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetSinglePurchaseOrderResponse
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetPurchaseOrdersResponse
import com.axelliant.hris.features.purchaseorders.data.remote.dto.PurchaseOrderListItemDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.PurchaseOrderSingleAddressDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.PurchaseOrderSingleDetailDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.PurchaseOrderSingleMasterDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.VendorDdlDto
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoVendorUi
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderDetailModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderPageResult
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderProductLine
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderStatus
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilization
import com.axelliant.hris.features.quotes.data.remote.dto.CustomerAddressDto
import com.axelliant.hris.features.quotes.domain.model.QuoteAddressUi
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

object PurchaseOrderApiMapper {
    private const val FALLBACK = "N/A"
    private const val API_STATUS_PENDING = 1
    private const val API_STATUS_RELEASED = 2
    private const val API_STATUS_CANCELED = 3
    private const val API_STATUS_APPROVED = 4
    private const val API_STATUS_DRAFT = 5

    fun mapPage(response: GetPurchaseOrdersResponse): PurchaseOrderPageResult {
        val orders = response.dataList.orEmpty().map(::toListModel)
        return PurchaseOrderPageResult(
            orders = orders,
            totalCount = response.totalCount ?: orders.size
        )
    }

    fun mapDetail(response: GetSinglePurchaseOrderResponse): PurchaseOrderDetailModel? {
        val master = response.masterData.orEmpty().firstOrNull() ?: return null
        val details = response.purchaseOrderDetail.orEmpty()
            .ifEmpty { master.purchaseOrderDetail.orEmpty() }
        val primaryDetail = details.firstOrNull()
        val status = (master.approvalStatus ?: master.status).toStatus()
        val total = master.totalAmount ?: details.sumOf { it.totalPrice ?: 0.0 }
        return PurchaseOrderDetailModel(
            id = master.id.orEmpty(),
            poNumber = master.poId.orFallback(),
            utilization = PurchaseOrderUtilization.PENDING,
            status = status,
            grandTotal = CurrencyFormatter.format(total),
            vendorName = primaryDetail?.vendorName.orFallback(),
            fulfillmentStatus = status,
            createdBy = master.customerName.orFallback(),
            createdDate = FALLBACK,
            billingAddress = master.billingAddress.toDisplayAddress(),
            shippingAddress = master.shippingAddress.toDisplayAddress(),
            products = details.map(::toProductLine),
            billingAddressId = master.billingAddressId.orEmpty(),
            shippingAddressId = master.shippingAddressId.orEmpty(),
            vendorId = primaryDetail?.vendorId.orEmpty(),
            subtotal = master.subTotal ?: 0.0,
            shipping = master.shipping ?: 0.0,
            tax = master.tax ?: 0.0,
            totalAmount = total,
            quoteIds = master.quoteIds.orEmpty()
                .ifEmpty { response.quoteDdl.orEmpty().mapNotNull { it.id?.takeIf(String::isNotBlank) } },
            saleOrderIds = response.saleOrderDdl.orEmpty()
                .mapNotNull { it.id?.takeIf(String::isNotBlank) },
            quoteSerialIds = master.quoteSerialIds.orEmpty()
                .ifEmpty { response.quoteDdl.orEmpty().mapNotNull { it.text?.takeIf(String::isNotBlank) } },
            soSerialIds = master.soSerialIds.orEmpty()
                .ifEmpty { response.saleOrderDdl.orEmpty().mapNotNull { it.text?.takeIf(String::isNotBlank) } },
            customerEmail = master.customerEmail.orEmpty(),
            customerName = master.customerName.orEmpty(),
            billingAddressData = master.billingAddress.toAddPoAddress(
                fallbackId = master.billingAddressId.orEmpty()
            ),
            shippingAddressData = master.shippingAddress.toAddPoAddress(
                fallbackId = master.shippingAddressId.orEmpty()
            )
        )
    }

    fun mapVendor(dto: VendorDdlDto): PoVendorUi? {
        val id = dto.id?.trim().orEmpty()
            .ifBlank { dto.vendorId?.trim().orEmpty() }
        val name = dto.name?.trim().orEmpty()
            .ifBlank { dto.vendorName?.trim().orEmpty() }
            .ifBlank { dto.code?.trim().orEmpty() }
        if (id.isBlank() && name.isBlank()) return null
        return PoVendorUi(
            id = id.ifBlank { name },
            name = name.ifBlank { id }
        )
    }

    fun mapBillingAddress(dto: CompanyAddressDto): PoAddressUi? {
        val id = dto.billingAddressId?.trim().orEmpty()
            .ifBlank { dto.addressId?.trim().orEmpty() }
            .ifBlank { dto.id?.trim().orEmpty() }
        return mapAddress(id, dto)
    }

    fun mapShippingAddress(dto: CompanyAddressDto): PoAddressUi? {
        val id = dto.shippingAddressId?.trim().orEmpty()
            .ifBlank { dto.addressId?.trim().orEmpty() }
            .ifBlank { dto.id?.trim().orEmpty() }
        return mapAddress(id, dto)
    }

    fun mapCustomerBilling(address: CustomerAddressDto): PoAddressUi? {
        val id = address.billingAddressId?.trim().orEmpty()
            .ifBlank { address.addressId?.trim().orEmpty() }
            .ifBlank { address.id?.trim().orEmpty() }
        return mapCustomerAddress(id, address)
    }

    fun mapCustomerShipping(address: CustomerAddressDto): PoAddressUi? {
        val id = address.shippingAddressId?.trim().orEmpty()
            .ifBlank { address.addressId?.trim().orEmpty() }
            .ifBlank { address.id?.trim().orEmpty() }
        return mapCustomerAddress(id, address)
    }

    fun mapQuoteAddress(address: QuoteAddressUi): PoAddressUi {
        return PoAddressUi(
            id = address.id,
            label = address.displayText.ifBlank { address.address },
            address = address.address,
            city = address.city,
            state = address.state,
            country = address.country,
            zipCode = address.zipCode
        )
    }

    fun extractVendorsFromProductFilters(payload: JsonElement): List<PoVendorUi> {
        val options = extractVendorOptionNames(payload)
        return options.map { name -> PoVendorUi(id = name, name = name) }
    }

    fun <T> unwrapSuccess(payload: BaseApiModel<T>): T? {
        return if (payload.data?.success == true) payload.data.data else null
    }

    fun resolveErrorMessage(result: ApiResult<*>): String {
        return when (result) {
            is ApiResult.HttpError -> parseHttpError(result)
            is ApiResult.NetworkError -> result.message
            is ApiResult.UnknownError -> result.message
            ApiResult.Unauthorized -> "Session expired. Please sign in again."
            ApiResult.Empty -> "No data found."
            is ApiResult.Success -> ""
        }
    }

    private fun parseHttpError(error: ApiResult.HttpError): String {
        val body = error.errorBody.orEmpty()
        if (body.isNotBlank()) {
            runCatching {
                val typed = Gson().fromJson<BaseApiModel<List<AddEditPurchaseOrderResponseDto>>>(
                    body,
                    object : TypeToken<BaseApiModel<List<AddEditPurchaseOrderResponseDto>>>() {}.type
                )
                typed?.data?.message?.takeIf { it.isNotBlank() }?.let { return it }
                typed?.message?.text?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return error.message.ifBlank { "Unable to complete request." }
    }

    private fun toListModel(dto: PurchaseOrderListItemDto): PurchaseOrderModel {
        return PurchaseOrderModel(
            id = dto.id.orEmpty(),
            poNumber = dto.poId.orFallback(),
            vendorName = dto.vendorName.orFallback(),
            status = dto.status.toStatus(),
            utilization = dto.utilizingStatus.toUtilization(),
            grandTotal = CurrencyFormatter.format(dto.totalAmount),
            deliveryDate = formatDateForUi(dto.createdDate)
        )
    }

    private fun toProductLine(dto: PurchaseOrderSingleDetailDto): PurchaseOrderProductLine {
        return PurchaseOrderProductLine(
            id = dto.id.orEmpty(),
            productId = dto.productId.orEmpty(),
            name = dto.productName?.takeIf { it.isNotBlank() }
                ?: dto.description.orFallback(),
            description = dto.description.orEmpty(),
            partNumber = dto.axePartNo.orFallback(),
            uom = dto.uom.orEmpty(),
            quantity = dto.quantity ?: 0,
            unitPrice = CurrencyFormatter.format(dto.unitPrice),
            lineTotal = CurrencyFormatter.format(dto.totalPrice),
            vendorId = dto.vendorId.orEmpty(),
            vendorName = dto.vendorName.orEmpty(),
            deliveryDate = formatDateForApi(dto.deliveryDate),
            saleOrderDetailIds = dto.saleOrderDetailIds.orEmpty(),
            quoteIds = dto.quoteIds.orEmpty()
        )
    }

    private fun Int?.toStatus(): PurchaseOrderStatus {
        return when (this) {
            API_STATUS_DRAFT -> PurchaseOrderStatus.DRAFT
            API_STATUS_PENDING -> PurchaseOrderStatus.PENDING
            API_STATUS_RELEASED -> PurchaseOrderStatus.RELEASED
            API_STATUS_CANCELED -> PurchaseOrderStatus.CANCELED
            API_STATUS_APPROVED -> PurchaseOrderStatus.APPROVED
            else -> PurchaseOrderStatus.PENDING
        }
    }

    private fun String?.toUtilization(): PurchaseOrderUtilization {
        val value = this.orEmpty()
        return when {
            value.equals("Partially Utilized", ignoreCase = true) -> PurchaseOrderUtilization.PARTIALLY_UTILIZED
            value.equals("Fully Utilized", ignoreCase = true) -> PurchaseOrderUtilization.FULLY_UTILIZED
            else -> PurchaseOrderUtilization.PENDING
        }
    }

    private fun formatDateForUi(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        parseDate(raw)?.let {
            return SimpleDateFormat("MM-dd-yyyy", Locale.US).format(it)
        }
        return raw
    }

    private fun formatDateForApi(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
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
            "MMM d, yyyy, h:mm:ss a",
            "MMM dd, yyyy, hh:mm:ss a"
        )
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(raw)
            }.getOrNull()
        }
    }

    private fun mapAddress(id: String, dto: CompanyAddressDto): PoAddressUi? {
        val address = dto.address?.trim().orEmpty()
        val name = dto.name?.trim().orEmpty()
        if (id.isBlank() && address.isBlank() && name.isBlank()) return null
        val city = dto.city?.trim().orEmpty()
        val state = (dto.state ?: dto.stateOrProvince)?.trim().orEmpty()
        val label = when {
            name.isNotBlank() && address.isNotBlank() -> "$name - $address"
            name.isNotBlank() -> name
            address.isNotBlank() && city.isNotBlank() -> "$address - $city"
            else -> address.ifBlank { name }
        }
        return PoAddressUi(
            id = id.ifBlank { label },
            label = label,
            address = address.ifBlank { name },
            city = city,
            state = state,
            country = dto.country?.trim().orEmpty(),
            zipCode = (dto.zipCode ?: dto.zip)?.trim().orEmpty()
        )
    }

    private fun mapCustomerAddress(id: String, address: CustomerAddressDto): PoAddressUi? {
        val line = address.address?.trim().orEmpty()
        if (id.isBlank() && line.isBlank()) return null
        val city = address.city?.trim().orEmpty()
        val state = (address.state ?: address.stateOrProvince)?.trim().orEmpty()
        val label = listOf(line, city, state)
            .filter { it.isNotEmpty() }
            .joinToString(" - ")
            .ifBlank { line }
        return PoAddressUi(
            id = id.ifBlank { label },
            label = label,
            address = line,
            city = city,
            state = state,
            country = address.country?.trim().orEmpty(),
            zipCode = (address.zipCode ?: address.zip)?.trim().orEmpty()
        )
    }

    private fun extractVendorOptionNames(payload: JsonElement): List<String> {
        val filters = extractFilterArray(payload)
        val vendorFilter = filters.firstOrNull { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@firstOrNull false
            val key = obj.firstString("key", "Key", "field", "Field", "name", "Name").orEmpty()
            key.equals("vendor", ignoreCase = true) || key.equals("VendorName", ignoreCase = true)
        }?.asJsonObject ?: return emptyList()

        val options = vendorFilter.get("options")
            ?: vendorFilter.get("Options")
            ?: vendorFilter.get("values")
            ?: vendorFilter.get("Values")
            ?: return emptyList()

        return when {
            options.isJsonArray -> options.asJsonArray.mapNotNull { option ->
                when {
                    option.isJsonPrimitive -> option.asString.trim().takeIf { it.isNotEmpty() }
                    option.isJsonObject -> option.asJsonObject.firstString(
                        "Name", "name", "label", "Label", "value", "Value"
                    )?.trim()?.takeIf { it.isNotEmpty() }
                    else -> null
                }
            }
            else -> emptyList()
        }.distinctBy { it.lowercase() }
    }

    private fun extractFilterArray(payload: JsonElement): List<JsonElement> {
        if (payload.isJsonArray) return payload.asJsonArray.toList()
        val obj = payload.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
        listOf("filters", "Filters", "data", "Data", "result", "Result", "items", "Items")
            .forEach { key ->
                obj.get(key)?.let { nested ->
                    extractFilterArray(nested).takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
        return emptyList()
    }

    private fun JsonObject.firstString(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { it.isJsonPrimitive }?.asString
        }
    }

    fun mapSingleBillingAddress(master: PurchaseOrderSingleMasterDto): PoAddressUi? {
        val id = master.billingAddressId.orEmpty()
            .ifBlank { master.billingAddress?.id.orEmpty() }
        return mapSingleAddress(id, master.billingAddress)
    }

    fun mapSingleShippingAddress(master: PurchaseOrderSingleMasterDto): PoAddressUi? {
        val id = master.shippingAddressId.orEmpty()
            .ifBlank { master.shippingAddress?.id.orEmpty() }
        return mapSingleAddress(id, master.shippingAddress)
    }

    private fun mapSingleAddress(
        id: String,
        address: PurchaseOrderSingleAddressDto?
    ): PoAddressUi? {
        val line = address?.address?.trim().orEmpty()
        val name = address?.name?.trim().orEmpty()
        if (id.isBlank() && line.isBlank() && name.isBlank()) return null
        val city = address?.city?.trim().orEmpty()
        val state = (address?.stateOrProvince ?: address?.state)?.trim().orEmpty()
        val label = listOf(name, line, city, state)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { line.ifBlank { name } }
        return PoAddressUi(
            id = id.ifBlank { label },
            label = label,
            address = line.ifBlank { name },
            city = city,
            state = state,
            country = address?.country?.trim().orEmpty(),
            zipCode = (address?.zip ?: address?.zipCode)?.trim().orEmpty()
        )
    }

    private fun PurchaseOrderSingleAddressDto?.toDisplayAddress(): String {
        if (this == null) return FALLBACK
        return listOf(
            name,
            title,
            address,
            listOf(city, stateOrProvince ?: state).filter { !it.isNullOrBlank() }.joinToString(", "),
            listOf(country, zip ?: zipCode).filter { !it.isNullOrBlank() }.joinToString(" - ")
        )
            .map { it.orEmpty().trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .ifBlank { FALLBACK }
    }

    private fun PurchaseOrderSingleAddressDto?.toAddPoAddress(fallbackId: String): AddPoAddressUi? {
        val addressId = this?.id.orEmpty().ifBlank { fallbackId }
        if (addressId.isBlank() && this == null) return null
        return AddPoAddressUi(
            id = addressId,
            accountId = this?.accountId.orEmpty(),
            address = this?.address.orEmpty(),
            country = this?.country.orEmpty(),
            stateOrProvince = this?.stateOrProvince ?: this?.state.orEmpty(),
            city = this?.city.orEmpty(),
            zip = this?.zip ?: this?.zipCode.orEmpty(),
            type = this?.type,
            isDiscarded = this?.isDiscarded ?: false
        )
    }

    private fun String?.orFallback(): String = this?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK
}
