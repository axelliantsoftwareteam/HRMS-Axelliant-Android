package com.axelliant.hris.features.purchaseorders.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.inventory.products.data.ProductsRepository
import com.axelliant.hris.features.purchaseorders.data.remote.PurchaseOrderApiService
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPosAddressDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPosPurchaseOrderDetailDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPosPurchaseOrderMasterDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPosPurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderLineDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetPurchaseOrdersRequest
import com.axelliant.hris.features.purchaseorders.domain.model.AddPoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.CreatePurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.domain.model.CreatePurchaseOrderResult
import com.axelliant.hris.features.purchaseorders.domain.model.EditPoProductLineUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoAddressUi
import com.axelliant.hris.features.purchaseorders.domain.model.PoVendorUi
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderHistoryItemUiModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderDetailModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderPageResult
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderProductLine
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderStatus
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilization
import com.axelliant.hris.features.purchaseorders.domain.model.UpdatePurchaseOrderRequest
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.data.remote.CreateQuoteApiService
import com.axelliant.hris.features.quotes.data.QuoteApiResponseParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Purchase Orders data source.
 * List, vendor list, company addresses, and AddEdit use Retrofit with resilient fallbacks.
 * Detail remains mock until GetById is available.
 */
@Singleton
class PurchaseOrdersRepository @Inject constructor(
    private val purchaseOrderApiService: PurchaseOrderApiService,
    private val createQuoteApiService: CreateQuoteApiService,
    private val productsRepository: ProductsRepository,
    private val quotesRepository: QuotesRepository,
    private val safeApiExecutor: SafeApiExecutor
) {

    private val createdOrders = mutableListOf<PurchaseOrderModel>()
    private val loadedRemoteOrders = mutableListOf<PurchaseOrderModel>()
    private val createdDetails = mutableMapOf<String, PurchaseOrderDetailModel>()
    private val nextPoSequence = AtomicInteger(100)

    suspend fun getPurchaseOrders(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = ""
    ): ApiResult<PurchaseOrderPageResult> {
        val request = GetPurchaseOrdersRequest(
            start = start,
            limit = limit,
            search = search.trim()
        )
        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.getAllPurchaseOrders(request)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val body = payload.data?.data
                if (payload.data?.success == true && body != null) {
                    val page = PurchaseOrderApiMapper.mapPage(body)
                    if (start == 0) {
                        loadedRemoteOrders.clear()
                    }
                    page.orders.forEach { order ->
                        loadedRemoteOrders.removeAll { it.id == order.id }
                        loadedRemoteOrders.add(order)
                    }
                    ApiResult.Success(
                        page.copy(
                            orders = if (start == 0) createdOrders + page.orders else page.orders,
                            totalCount = page.totalCount + if (start == 0) createdOrders.size else 0
                        )
                    )
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Unable to load purchase orders.")
                }
            }
            ApiResult.Empty -> ApiResult.UnknownError("No purchase orders found.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getPurchaseOrderDetail(orderId: String): ApiResult<PurchaseOrderDetailModel> {
        createdDetails[orderId]?.let { return ApiResult.Success(it) }
        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.getSinglePurchaseOrder(orderId)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val detail = payload.data?.data?.let(PurchaseOrderApiMapper::mapDetail)
                if (payload.data?.success == true && detail != null) {
                    ApiResult.Success(detail)
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Purchase order not found.")
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Purchase order not found.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getPurchaseOrderHistory(
        purchaseOrderId: String
    ): ApiResult<List<PurchaseOrderHistoryItemUiModel>> {
        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.getPurchaseOrderComments(purchaseOrderId)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val comments = payload.data?.data
                if (payload.data?.success == true && comments != null) {
                    ApiResult.Success(PurchaseOrderHistoryMapper.map(comments))
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Unable to load history.")
                }
            }
            ApiResult.Empty -> ApiResult.Success(emptyList())
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun <T> apiMessage(
        payload: com.axelliant.hris.core.network.BaseApiModel<T>
    ): String? {
        return payload.data?.message?.takeIf { it.isNotBlank() }
            ?: payload.message?.text?.takeIf { it.isNotBlank() }
    }

    suspend fun createPurchaseOrder(
        request: CreatePurchaseOrderRequest
    ): ApiResult<CreatePurchaseOrderResult> {
        return if (request.isManual) {
            createManualPurchaseOrder(request)
        } else {
            createPosPurchaseOrder(request)
        }
    }

    private suspend fun createManualPurchaseOrder(
        request: CreatePurchaseOrderRequest
    ): ApiResult<CreatePurchaseOrderResult> {
        val selected = request.products.filter { it.isSelected }
        if (selected.isEmpty()) {
            return ApiResult.UnknownError("Select at least one product.")
        }

        val vendorOptions = (getVendors() as? ApiResult.Success)?.data.orEmpty()
        val primaryLine = selected.first()
        val vendor = resolveVendor(primaryLine.vendorId, primaryLine.vendor, vendorOptions)
            ?: vendorOptions.firstOrNull()
            ?: return ApiResult.UnknownError("No vendors available.")
        val addresses = when (val addressResult = getCompanyAddresses()) {
            is ApiResult.Success -> addressResult.data
            is ApiResult.HttpError -> return ApiResult.HttpError(
                code = addressResult.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(addressResult),
                errorBody = addressResult.errorBody
            )
            is ApiResult.NetworkError -> return addressResult
            is ApiResult.UnknownError -> return addressResult
            ApiResult.Unauthorized -> return ApiResult.Unauthorized
            ApiResult.Empty -> return ApiResult.UnknownError("No company addresses available.")
        }
        val billing = addresses.first.firstOrNull()
            ?: return ApiResult.UnknownError("No billing address available.")
        val shipping = addresses.second.firstOrNull()
            ?: return ApiResult.UnknownError("No shipping address available.")

        val apiRequest = AddEditPurchaseOrderRequest(
            id = null,
            poNumber = "",
            vendorId = vendor.id,
            billingAddressId = billing.id,
            shippingAddressId = shipping.id,
            subTotal = request.subtotal,
            shippingAmount = request.shipping,
            taxAmount = request.tax,
            grandTotal = request.grandTotal,
            lineItems = selected.map { line ->
                AddEditPurchaseOrderLineDto(
                    id = null,
                    productId = line.productId.ifBlank { line.id }.takeIf { it.isNotBlank() },
                    name = line.name,
                    sku = line.axePart,
                    quantity = line.quantity.coerceAtLeast(1),
                    unitCost = line.unitCost,
                    lineTotal = line.lineTotal
                )
            }
        )

        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.addEditPurchaseOrder(apiRequest)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val serverMessage = apiMessage(payload)
                if (payload.data?.success == true) {
                    val response = payload.data.data.orEmpty().firstOrNull()
                    val poNumber = response?.poId
                        ?: response?.poNumber
                        ?: "Purchase order"
                    val created = PurchaseOrderModel(
                        id = response?.id.orEmpty().ifBlank { UUID.randomUUID().toString() },
                        poNumber = poNumber,
                        vendorName = vendor.name,
                        status = PurchaseOrderStatus.DRAFT,
                        utilization = PurchaseOrderUtilization.PENDING,
                        grandTotal = NumberFormat.getCurrencyInstance(Locale.US).format(request.grandTotal),
                        deliveryDate = currentUiDate()
                    )
                    createdOrders.add(0, created)
                    ApiResult.Success(
                        CreatePurchaseOrderResult(
                            purchaseOrder = created,
                            message = serverMessage ?: response?.message
                            ?: "Purchase order created successfully."
                        )
                    )
                } else {
                    ApiResult.UnknownError(serverMessage ?: "Unable to create purchase order.")
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Unable to create purchase order.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private suspend fun createPosPurchaseOrder(
        request: CreatePurchaseOrderRequest
    ): ApiResult<CreatePurchaseOrderResult> {
        val selected = request.products.filter { it.isSelected }
        if (request.quotationId.isBlank()) {
            return ApiResult.UnknownError("Quote is required.")
        }
        if (request.saleOrderId.isBlank()) {
            return ApiResult.UnknownError("Sales order is required.")
        }
        if (selected.isEmpty()) {
            return ApiResult.UnknownError("Select at least one product.")
        }

        val vendorOptions = (getVendors() as? ApiResult.Success)?.data.orEmpty()
        val fallbackVendor = vendorOptions.firstOrNull()
        val info = request.saleOrderInfo
        val apiRequest = AddEditPosPurchaseOrderRequest(
            masterData = listOf(
                AddEditPosPurchaseOrderMasterDto(
                    id = UUID.randomUUID().toString(),
                    quoteIds = listOf(request.quotationId),
                    subTotal = request.subtotal,
                    shipping = request.shipping,
                    tax = request.tax,
                    totalAmount = request.grandTotal,
                    billingAddressId = info?.billingAddressId.orEmpty(),
                    shippingAddressId = info?.shippingAddressId.orEmpty(),
                    saleOrderId = listOf(request.saleOrderId),
                    customerEmail = info?.customerEmail.orEmpty(),
                    customerName = request.customerName,
                    billingAddress = info?.billingAddress.toPosAddressDto(),
                    shippingAddress = info?.shippingAddress.toPosAddressDto(),
                    soSerialIds = listOf(request.saleOrderNumber)
                )
            ),
            purchaseOrderDetail = selected.map { line ->
                val vendor = resolveVendor(line.vendorId, line.vendor, vendorOptions)
                    ?: fallbackVendor
                AddEditPosPurchaseOrderDetailDto(
                    productId = line.productId.ifBlank { line.id }.takeIf { it.isNotBlank() },
                    quantity = line.quantity.coerceAtLeast(1),
                    unitPrice = line.unitCost,
                    totalPrice = line.lineTotal,
                    vendorId = vendor?.id.orEmpty().ifBlank { line.vendorId },
                    productName = line.name,
                    description = line.description,
                    vendorName = vendor?.name ?: line.vendor.replace('_', ' '),
                    uom = line.uom,
                    axePartNo = line.axePart,
                    saleOrderDetailIds = line.saleOrderDetailIds.ifEmpty { listOf(line.id) },
                    quoteIds = line.quoteIds,
                    deliveryDate = line.deliveryDate.ifBlank { currentApiDate() }
                )
            }
        )

        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.addEditPosPurchaseOrder(apiRequest)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val serverMessage = apiMessage(payload)
                if (payload.data?.success == true) {
                    val response = payload.data.data.orEmpty().firstOrNull()
                    val poNumber = response?.poId
                        ?: response?.poNumber
                        ?: "Purchase order"
                    val created = PurchaseOrderModel(
                        id = response?.id.orEmpty().ifBlank { UUID.randomUUID().toString() },
                        poNumber = poNumber,
                        vendorName = selected.firstOrNull()?.vendor?.replace('_', ' ') ?: "-",
                        status = PurchaseOrderStatus.DRAFT,
                        utilization = PurchaseOrderUtilization.PENDING,
                        grandTotal = NumberFormat.getCurrencyInstance(Locale.US).format(request.grandTotal),
                        deliveryDate = currentUiDate()
                    )
                    createdOrders.add(0, created)
                    ApiResult.Success(
                        CreatePurchaseOrderResult(
                            purchaseOrder = created,
                            message = serverMessage ?: response?.message
                            ?: "Purchase order created successfully."
                        )
                    )
                } else {
                    ApiResult.UnknownError(serverMessage ?: "Unable to create purchase order.")
                }
            }
            is ApiResult.Empty -> ApiResult.UnknownError("Unable to create purchase order.")
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun AddPoAddressUi?.toPosAddressDto(): AddEditPosAddressDto? {
        if (this == null) return null
        return AddEditPosAddressDto(
            id = id,
            accountId = accountId,
            address = address,
            country = country,
            stateOrProvince = stateOrProvince,
            city = city,
            zip = zip,
            type = type,
            isDiscarded = isDiscarded
        )
    }

    private fun resolveVendor(
        vendorId: String,
        vendorName: String,
        vendors: List<PoVendorUi>
    ): PoVendorUi? {
        if (vendorId.isNotBlank()) {
            vendors.firstOrNull { it.id == vendorId }?.let { return it }
        }
        val normalizedName = normalizeVendor(vendorName)
        return vendors.firstOrNull { normalizeVendor(it.name) == normalizedName }
            ?: vendors.firstOrNull { normalizeVendor(it.id) == normalizedName }
    }

    private fun normalizeVendor(value: String): String {
        return value.replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase(Locale.US)
    }

    private fun currentApiDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun currentUiDate(): String {
        return SimpleDateFormat("MM-dd-yyyy", Locale.US).format(Date())
    }

    private fun buildDetail(order: PurchaseOrderModel): PurchaseOrderDetailModel {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val products = when (order.id) {
            "7", "11" -> listOf(
                PurchaseOrderProductLine(
                    id = "${order.id}-p1",
                    name = "Dell Latitude 5440 - 14\" Laptop",
                    partNumber = "DELL-LAT-5440",
                    quantity = 1,
                    unitPrice = "$89.99",
                    lineTotal = "$89.99"
                ),
                PurchaseOrderProductLine(
                    id = "${order.id}-p2",
                    name = "Logitech MX Master 3S Wireless",
                    partNumber = "LOG-MX-3S",
                    quantity = 1,
                    unitPrice = "$22.98",
                    lineTotal = "$22.98"
                )
            )
            else -> listOf(
                PurchaseOrderProductLine(
                    id = "${order.id}-p1",
                    name = "1M USB A/A EXT CBL BLK",
                    partNumber = "AXE-0006900",
                    quantity = 2,
                    unitPrice = currency.format(
                        parseCurrency(order.grandTotal) / 2.0
                    ),
                    lineTotal = order.grandTotal
                )
            )
        }
        val displayTotal = when (order.id) {
            "7", "11" -> "$112.97"
            else -> order.grandTotal
        }
        return PurchaseOrderDetailModel(
            id = order.id,
            poNumber = if (order.id == "7") "PO-00000126" else order.poNumber,
            utilization = order.utilization,
            status = order.status,
            grandTotal = displayTotal,
            vendorName = order.vendorName,
            fulfillmentStatus = when (order.status) {
                PurchaseOrderStatus.RELEASED, PurchaseOrderStatus.APPROVED -> order.status
                else -> PurchaseOrderStatus.PENDING
            },
            createdBy = DEFAULT_CREATED_BY,
            createdDate = formatCreatedDate(order.deliveryDate),
            billingAddress = DEFAULT_BILLING_ADDRESS,
            shippingAddress = DEFAULT_SHIPPING_ADDRESS,
            products = products
        )
    }

    private fun formatCreatedDate(deliveryDate: String): String {
        return try {
            val input = SimpleDateFormat("MM-dd-yyyy", Locale.US)
            val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
            output.format(input.parse(deliveryDate)!!)
        } catch (_: Exception) {
            "Oct 24, 2023"
        }
    }

    private fun parseCurrency(value: String): Double {
        return value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }

    suspend fun getVendors(search: String = ""): ApiResult<List<PoVendorUi>> {
        val remote = when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.getVendorsForDdl(search = search.trim())
        }) {
            is ApiResult.Success -> {
                val vendors = PurchaseOrderApiMapper.unwrapSuccess(result.data)
                    .orEmpty()
                    .mapNotNull(PurchaseOrderApiMapper::mapVendor)
                    .filter {
                        search.isBlank() ||
                            it.name.contains(search, ignoreCase = true)
                    }
                if (vendors.isNotEmpty()) ApiResult.Success(vendors) else null
            }
            else -> null
        }
        if (remote != null) return remote

        return when (val filters = productsRepository.getAdvancedProductFilters()) {
            is ApiResult.Success -> {
                val vendors = PurchaseOrderApiMapper.extractVendorsFromProductFilters(filters.data)
                    .filter {
                        search.isBlank() ||
                            it.name.contains(search, ignoreCase = true)
                    }
                    .ifEmpty { FALLBACK_VENDORS }
                ApiResult.Success(vendors)
            }
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = filters.code,
                message = PurchaseOrderApiMapper.resolveErrorMessage(filters),
                errorBody = filters.errorBody
            )
            is ApiResult.NetworkError -> filters
            is ApiResult.UnknownError -> filters
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.Empty -> ApiResult.Success(FALLBACK_VENDORS)
        }
    }

    suspend fun getCompanyAddresses(): ApiResult<Pair<List<PoAddressUi>, List<PoAddressUi>>> {
        when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.getCompanyAddressesForDdl()
        }) {
            is ApiResult.Success -> {
                val payload = PurchaseOrderApiMapper.unwrapSuccess(result.data)
                val billing = payload?.billingAddress.orEmpty()
                    .mapNotNull(PurchaseOrderApiMapper::mapBillingAddress)
                val shipping = payload?.shippingAddress.orEmpty()
                    .mapNotNull(PurchaseOrderApiMapper::mapShippingAddress)
                if (billing.isNotEmpty() || shipping.isNotEmpty()) {
                    return ApiResult.Success(billing to shipping)
                }
            }
            else -> Unit
        }

        return loadAddressesFromCompanyAccount()
    }

    private suspend fun loadAddressesFromCompanyAccount(): ApiResult<Pair<List<PoAddressUi>, List<PoAddressUi>>> {
        val accounts = when (
            val search = quotesRepository.searchCustomers(search = COMPANY_ACCOUNT_SEARCH)
        ) {
            is ApiResult.Success -> search.data
            else -> emptyList()
        }
        val company = accounts.firstOrNull {
            it.name.contains("Axelliant", ignoreCase = true)
        } ?: accounts.firstOrNull()

        if (company != null) {
            when (val details = safeApiExecutor.execute {
                createQuoteApiService.getCustomerDetailsForQuotation(accountId = company.id)
            }) {
                is ApiResult.Success -> {
                    val payload = QuoteApiResponseParser.unwrapSuccess(details.data)
                    val billing = payload?.billingAddress.orEmpty()
                        .mapNotNull(PurchaseOrderApiMapper::mapCustomerBilling)
                    val shipping = payload?.shippingAddress.orEmpty()
                        .mapNotNull(PurchaseOrderApiMapper::mapCustomerShipping)
                    if (billing.isNotEmpty() || shipping.isNotEmpty()) {
                        return ApiResult.Success(billing to shipping)
                    }
                }
                else -> Unit
            }
        }

        return ApiResult.Success(FALLBACK_BILLING_ADDRESSES to FALLBACK_SHIPPING_ADDRESSES)
    }

    suspend fun updatePurchaseOrder(
        request: UpdatePurchaseOrderRequest
    ): ApiResult<String> {
        val apiRequest = AddEditPosPurchaseOrderRequest(
            masterData = listOf(
                AddEditPosPurchaseOrderMasterDto(
                    id = request.id,
                    poId = request.poNumber,
                    quoteIds = request.quoteIds,
                    subTotal = request.subtotal,
                    shipping = request.shipping,
                    tax = request.tax,
                    totalAmount = request.grandTotal,
                    billingAddressId = request.billingAddressId,
                    shippingAddressId = request.shippingAddressId,
                    saleOrderId = request.saleOrderIds,
                    customerEmail = request.customerEmail,
                    customerName = request.customerName,
                    billingAddress = request.billingAddress.toPosAddressDto(),
                    shippingAddress = request.shippingAddress.toPosAddressDto(),
                    quoteSerialIds = request.quoteSerialIds,
                    soSerialIds = request.soSerialIds
                )
            ),
            purchaseOrderDetail = request.products.map { line ->
                AddEditPosPurchaseOrderDetailDto(
                    id = line.id.takeIf { it.isNotBlank() },
                    purchaseOrderId = request.id,
                    productId = line.productId.ifBlank { line.id }.takeIf { it.isNotBlank() },
                    quantity = line.quantity.coerceAtLeast(1),
                    unitPrice = line.unitCost,
                    totalPrice = line.lineTotal,
                    vendorId = request.vendorId.ifBlank { line.vendorId },
                    productName = line.name,
                    description = line.description,
                    vendorName = request.vendorName.ifBlank { line.vendorName },
                    uom = line.uom.ifBlank { "Each" },
                    axePartNo = line.sku,
                    saleOrderDetailIds = line.saleOrderDetailIds,
                    quoteIds = line.quoteIds,
                    deliveryDate = line.deliveryDate.ifBlank { currentApiDate() }
                )
            }
        )

        return when (val result = safeApiExecutor.execute {
            purchaseOrderApiService.addEditPosPurchaseOrder(apiRequest)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val response = PurchaseOrderApiMapper.unwrapSuccess(payload)
                    ?.firstOrNull()
                if (payload.data?.success == true) {
                    val message = response?.message
                        ?.takeIf { it.isNotBlank() }
                        ?: apiMessage(payload)
                        ?: "Purchase order ${request.poNumber} updated."
                    persistLocalUpdate(request)
                    ApiResult.Success(message)
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Unable to update purchase order.")
                }
            }
            is ApiResult.HttpError -> {
                ApiResult.HttpError(
                    code = result.code,
                    message = PurchaseOrderApiMapper.resolveErrorMessage(result),
                    errorBody = result.errorBody
                )
            }
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.Empty -> ApiResult.UnknownError("Unable to update purchase order.")
        }
    }

    private fun persistLocalUpdate(request: UpdatePurchaseOrderRequest) {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val existing = (createdOrders + SAMPLE_ORDERS).firstOrNull { it.id == request.id }
        val updated = PurchaseOrderModel(
            id = request.id,
            poNumber = request.poNumber,
            vendorName = request.vendorName,
            status = existing?.status ?: PurchaseOrderStatus.DRAFT,
            utilization = existing?.utilization ?: PurchaseOrderUtilization.PENDING,
            grandTotal = currency.format(request.grandTotal),
            deliveryDate = existing?.deliveryDate
                ?: SimpleDateFormat("MM-dd-yyyy", Locale.US).format(Date())
        )
        createdOrders.removeAll { it.id == request.id }
        createdOrders.add(0, updated)
        createdDetails[request.id] = PurchaseOrderDetailModel(
            id = request.id,
            poNumber = request.poNumber,
            utilization = updated.utilization,
            status = updated.status,
            grandTotal = updated.grandTotal,
            vendorName = request.vendorName,
            fulfillmentStatus = PurchaseOrderStatus.PENDING,
            createdBy = DEFAULT_CREATED_BY,
            createdDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()),
            billingAddress = request.billingAddressLabel,
            shippingAddress = request.shippingAddressLabel,
            products = request.products.map { line ->
                PurchaseOrderProductLine(
                    id = line.id,
                    productId = line.productId,
                    name = line.name,
                    description = line.description,
                    partNumber = line.sku,
                    uom = line.uom,
                    quantity = line.quantity,
                    unitPrice = currency.format(line.unitCost),
                    lineTotal = currency.format(line.lineTotal),
                    vendorId = line.vendorId.ifBlank { request.vendorId },
                    vendorName = line.vendorName.ifBlank { request.vendorName },
                    deliveryDate = line.deliveryDate,
                    saleOrderDetailIds = line.saleOrderDetailIds,
                    quoteIds = line.quoteIds
                )
            }
        )
    }

    fun toEditProducts(detail: PurchaseOrderDetailModel): List<EditPoProductLineUi> {
        val mapped = detail.products.map { line ->
            EditPoProductLineUi(
                id = line.id,
                productId = line.productId,
                name = line.name,
                description = line.description,
                sku = line.partNumber,
                uom = line.uom,
                quantity = line.quantity,
                unitCost = parseCurrency(line.unitPrice),
                vendorId = line.vendorId,
                vendorName = line.vendorName,
                deliveryDate = line.deliveryDate,
                saleOrderDetailIds = line.saleOrderDetailIds,
                quoteIds = line.quoteIds
            )
        }
        if (mapped.isNotEmpty()) {
            return mapped
        }
        // Seed the Edit PO form with the design sample lines when mock detail is sparse.
        return listOf(
            EditPoProductLineUi(
                id = "${detail.id}-p1",
                name = "Dell UltraSharp 32\" 4K USB-C Hub Monitor",
                sku = "U3223QE | SKU: DE-MON-4K-32",
                quantity = 5,
                unitCost = 799.0
            ),
            EditPoProductLineUi(
                id = "${detail.id}-p2",
                name = "Logitech MX Master 3S Wireless Mouse",
                sku = "MX MASTER 3S | SKU: LO-MOU-MX3S",
                quantity = 12,
                unitCost = 99.0
            )
        )
    }

    companion object {
        const val PAGE_SIZE = 10
        private const val COMPANY_ACCOUNT_SEARCH = "Axelliant"
        private const val DEFAULT_CREATED_BY = "Haroon Tahir"
        private const val DEFAULT_BILLING_ADDRESS =
            "Axelliant Headquarters\n21250 Hawthorne Blvd\nSuite 500\nTorrance, CA 90503"
        private const val DEFAULT_SHIPPING_ADDRESS =
            "Regional Logistics Hub\n450 Golden Gate Ave\nSan Francisco, CA 94102"

        private val FALLBACK_VENDORS = listOf(
            PoVendorUi("global-tech", "Global Tech Solutions Inc."),
            PoVendorUi("td-synnex", "TD SYNNEX"),
            PoVendorUi("ingram", "Ingram Micro"),
            PoVendorUi("arrow", "Arrow Electronics")
        )

        private val FALLBACK_BILLING_ADDRESSES = listOf(
            PoAddressUi(
                id = "bill-hq",
                label = "Axelliant HQ - 2121 Rosecrans Ave",
                address = "2121 Rosecrans Ave",
                city = "El Segundo",
                state = "CA",
                country = "USA",
                zipCode = "90245"
            ),
            PoAddressUi(
                id = "bill-hq-hawthorne",
                label = "Axelliant Headquarters - 21250 Hawthorne Blvd",
                address = "21250 Hawthorne Blvd, Suite 500",
                city = "Torrance",
                state = "CA",
                country = "USA",
                zipCode = "90503"
            )
        )

        private val FALLBACK_SHIPPING_ADDRESSES = listOf(
            PoAddressUi(
                id = "ship-west",
                label = "Warehouse West - Suite 400",
                address = "Warehouse West, Suite 400",
                city = "San Francisco",
                state = "CA",
                country = "USA",
                zipCode = "94102"
            ),
            PoAddressUi(
                id = "ship-hub",
                label = "Regional Logistics Hub - 450 Golden Gate Ave",
                address = "450 Golden Gate Ave",
                city = "San Francisco",
                state = "CA",
                country = "USA",
                zipCode = "94102"
            )
        )

        private val SAMPLE_ORDERS = listOf(
            PurchaseOrderModel("1", "PO-2026-001", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$15.98", "07-07-2026"),
            PurchaseOrderModel("2", "PO-2026-001", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$15.98", "07-07-2026"),
            PurchaseOrderModel("3", "PO-2026-001", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$15.98", "07-07-2026"),
            PurchaseOrderModel("4", "PO-2026-001", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$15.98", "07-07-2026"),
            PurchaseOrderModel("5", "PO-2026-002", "Ingram Micro", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$42.50", "07-08-2026"),
            PurchaseOrderModel("6", "PO-2026-003", "Tech Data", PurchaseOrderStatus.PENDING, PurchaseOrderUtilization.PENDING, "$128.00", "07-10-2026"),
            PurchaseOrderModel("7", "PO-2026-004", "TD SYNNEX", PurchaseOrderStatus.APPROVED, PurchaseOrderUtilization.PARTIALLY_UTILIZED, "$112.97", "10-24-2023"),
            PurchaseOrderModel("8", "PO-2026-005", "TD SYNNEX", PurchaseOrderStatus.DRAFT, PurchaseOrderUtilization.PENDING, "$64.10", "07-14-2026"),
            PurchaseOrderModel("9", "PO-2026-006", "Avnet", PurchaseOrderStatus.CANCELED, PurchaseOrderUtilization.PENDING, "$210.00", "07-15-2026"),
            PurchaseOrderModel("10", "PO-2026-007", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$33.40", "07-16-2026"),
            PurchaseOrderModel("11", "PO-2026-008", "Ingram Micro", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.PARTIALLY_UTILIZED, "$55.75", "07-18-2026"),
            PurchaseOrderModel("12", "PO-2026-009", "TD SYNNEX", PurchaseOrderStatus.RELEASED, PurchaseOrderUtilization.FULLY_UTILIZED, "$15.98", "07-20-2026")
        )
    }
}
