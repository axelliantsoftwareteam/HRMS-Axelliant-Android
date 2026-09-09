package com.axelliant.hris.features.warehouse.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.warehouse.data.remote.WarehouseApiService
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseLocationRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetInventoryRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetInventoryTransactionsRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetWarehouseReceiptsRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetWarehousesRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.InventoryDto
import com.axelliant.hris.features.warehouse.data.remote.dto.InventoryTransactionDto
import com.axelliant.hris.features.warehouse.data.remote.dto.PurchaseOrderDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseLocationDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseLocationDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseReceiptLineDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseReceiptDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseReceiptSingleDto
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import com.axelliant.hris.features.warehouse.domain.model.InventoryPageResult
import com.axelliant.hris.features.warehouse.domain.model.InventoryTransactionModel
import com.axelliant.hris.features.warehouse.domain.model.InventoryTransactionsResult
import com.axelliant.hris.features.warehouse.domain.model.PurchaseOrderOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehousePageResult
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptDetailModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptLineModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptsPageResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WarehouseRepository @Inject constructor(
    private val apiService: WarehouseApiService,
    private val safeApiExecutor: SafeApiExecutor
) {
    suspend fun getWarehouses(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = ""
    ): ApiResult<WarehousePageResult> = withContext(Dispatchers.IO) {
        val request = GetWarehousesRequest(
            start = start,
            limit = limit,
            search = search
        )
        when (val result = safeApiExecutor.execute { apiService.getWarehouses(request) }) {
            is ApiResult.Success -> mapResponse(result.data)
            ApiResult.Empty -> ApiResult.Success(WarehousePageResult(emptyList(), 0))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getLocationTree(warehouseId: String): ApiResult<List<WarehouseLocationModel>> =
        withContext(Dispatchers.IO) {
            when (val result = safeApiExecutor.execute { apiService.getLocationTree(warehouseId) }) {
                is ApiResult.Success -> {
                    val payload = result.data
                    if (payload.data?.success == true) {
                        ApiResult.Success(payload.data.data.orEmpty().map { it.toModel() })
                    } else {
                        ApiResult.UnknownError(
                            payload.data?.message?.takeIf { it.isNotBlank() }
                                ?: payload.message?.text?.takeIf { it.isNotBlank() }
                                ?: "Unable to load locations."
                        )
                    }
                }
                ApiResult.Empty -> ApiResult.Success(emptyList())
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
                ApiResult.Unauthorized -> ApiResult.Unauthorized
            }
        }

    suspend fun getLocationDdl(warehouseId: String): ApiResult<List<WarehouseLocationOptionModel>> =
        withContext(Dispatchers.IO) {
            when (val result = safeApiExecutor.execute { apiService.getLocationDdl(warehouseId) }) {
                is ApiResult.Success -> {
                    val payload = result.data
                    if (payload.data?.success == true) {
                        ApiResult.Success(payload.data.data.orEmpty().map { it.toOptionModel() })
                    } else {
                        ApiResult.UnknownError(
                            payload.data?.message?.takeIf { it.isNotBlank() }
                                ?: payload.message?.text?.takeIf { it.isNotBlank() }
                                ?: "Unable to load parent locations."
                        )
                    }
                }
                ApiResult.Empty -> ApiResult.Success(emptyList())
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
                ApiResult.Unauthorized -> ApiResult.Unauthorized
            }
        }

    suspend fun getWarehouseDdl(search: String = ""): ApiResult<List<WarehouseOptionModel>> =
        withContext(Dispatchers.IO) {
            when (val result = safeApiExecutor.execute { apiService.getWarehouseDdl(search) }) {
                is ApiResult.Success -> {
                    val payload = result.data
                    if (payload.data?.success == true) {
                        ApiResult.Success(payload.data.data.orEmpty().map { it.toOptionModel() })
                    } else {
                        ApiResult.UnknownError(
                            payload.data?.message?.takeIf { it.isNotBlank() }
                                ?: payload.message?.text?.takeIf { it.isNotBlank() }
                                ?: "Unable to load warehouses."
                        )
                    }
                }
                ApiResult.Empty -> ApiResult.Success(emptyList())
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
                ApiResult.Unauthorized -> ApiResult.Unauthorized
            }
        }

    suspend fun getPurchaseOrderDdl(): ApiResult<List<PurchaseOrderOptionModel>> =
        withContext(Dispatchers.IO) {
            when (val result = safeApiExecutor.execute { apiService.getPurchaseOrderDdl() }) {
                is ApiResult.Success -> {
                    val payload = result.data
                    if (payload.data?.success == true) {
                        ApiResult.Success(payload.data.data.orEmpty().map { it.toOptionModel() })
                    } else {
                        ApiResult.UnknownError(
                            payload.data?.message?.takeIf { it.isNotBlank() }
                                ?: payload.message?.text?.takeIf { it.isNotBlank() }
                                ?: "Unable to load purchase orders."
                        )
                    }
                }
                ApiResult.Empty -> ApiResult.Success(emptyList())
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
                ApiResult.Unauthorized -> ApiResult.Unauthorized
            }
        }

    suspend fun getInventory(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = "",
        warehouseId: String? = null,
        locationId: String? = null,
        purchaseOrderId: String? = null,
        ownershipType: Int? = null,
        purpose: Int? = null,
        inventoryStatus: Int? = null,
        includeDepleted: Boolean = true
    ): ApiResult<InventoryPageResult> = withContext(Dispatchers.IO) {
        val request = GetInventoryRequest(
            start = start,
            limit = limit,
            search = search,
            warehouseId = warehouseId,
            locationId = locationId,
            purchaseOrderId = purchaseOrderId,
            ownershipType = ownershipType,
            purpose = purpose,
            inventoryStatus = inventoryStatus,
            includeDepleted = includeDepleted
        )
        when (val result = safeApiExecutor.execute { apiService.getInventory(request) }) {
            is ApiResult.Success -> mapInventoryResponse(result.data)
            ApiResult.Empty -> ApiResult.Success(InventoryPageResult(emptyList(), 0))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getInventoryTransactions(
        inventoryId: String
    ): ApiResult<InventoryTransactionsResult> = withContext(Dispatchers.IO) {
        val request = GetInventoryTransactionsRequest(inventoryId = inventoryId)
        when (val result = safeApiExecutor.execute { apiService.getInventoryTransactions(request) }) {
            is ApiResult.Success -> mapInventoryTransactionsResponse(result.data)
            ApiResult.Empty -> ApiResult.Success(InventoryTransactionsResult(emptyList(), 0))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getWarehouseReceipts(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = "",
        warehouseId: String? = null,
        purchaseOrderId: String? = null,
        receiptType: Int? = null,
        receiptStatus: Int? = null
    ): ApiResult<WarehouseReceiptsPageResult> = withContext(Dispatchers.IO) {
        val request = GetWarehouseReceiptsRequest(
            start = start,
            limit = limit,
            search = search,
            warehouseId = warehouseId,
            purchaseOrderId = purchaseOrderId,
            receiptType = receiptType,
            receiptStatus = receiptStatus
        )
        when (val result = safeApiExecutor.execute { apiService.getWarehouseReceipts(request) }) {
            is ApiResult.Success -> mapWarehouseReceiptsResponse(result.data)
            ApiResult.Empty -> ApiResult.Success(WarehouseReceiptsPageResult(emptyList(), 0))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getWarehouseReceiptDetail(
        receiptId: String
    ): ApiResult<WarehouseReceiptDetailModel> = withContext(Dispatchers.IO) {
        when (val result = safeApiExecutor.execute { apiService.getWarehouseReceipt(receiptId) }) {
            is ApiResult.Success -> mapWarehouseReceiptDetailResponse(result.data)
            ApiResult.Empty -> ApiResult.UnknownError("Warehouse receipt not found.")
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun addWarehouse(request: AddWarehouseRequest): ApiResult<WarehouseModel> =
        withContext(Dispatchers.IO) {
            when (val result = safeApiExecutor.execute { apiService.addWarehouse(request) }) {
                is ApiResult.Success -> {
                    val payload = result.data
                    val warehouse = payload.data?.data
                    if (payload.data?.success == true && warehouse != null) {
                        ApiResult.Success(warehouse.toModel())
                    } else {
                        ApiResult.UnknownError(
                            payload.data?.message?.takeIf { it.isNotBlank() }
                                ?: payload.message?.text?.takeIf { it.isNotBlank() }
                                ?: "Unable to save warehouse."
                        )
                    }
                }
                ApiResult.Empty -> ApiResult.UnknownError("Unable to save warehouse.")
                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
                ApiResult.Unauthorized -> ApiResult.Unauthorized
            }
        }

    suspend fun addLocation(
        request: AddWarehouseLocationRequest
    ): ApiResult<WarehouseLocationModel> = withContext(Dispatchers.IO) {
        when (val result = safeApiExecutor.execute { apiService.addLocation(request) }) {
            is ApiResult.Success -> {
                val payload = result.data
                val location = payload.data?.data
                if (payload.data?.success == true && location != null) {
                    ApiResult.Success(location.toModel())
                } else {
                    ApiResult.UnknownError(
                        payload.data?.message?.takeIf { it.isNotBlank() }
                            ?: payload.message?.text?.takeIf { it.isNotBlank() }
                            ?: "Unable to save location."
                    )
                }
            }
            ApiResult.Empty -> ApiResult.UnknownError("Unable to save location.")
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun mapResponse(
        payload: BaseApiModel<List<WarehouseDto>>
    ): ApiResult<WarehousePageResult> {
        val data = payload.data?.data.orEmpty()
        return if (payload.data?.success == true) {
            ApiResult.Success(
                WarehousePageResult(
                    warehouses = data.map { it.toModel() },
                    totalCount = data.firstOrNull()?.totalCount ?: data.size
                )
            )
        } else {
            ApiResult.UnknownError(
                payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: payload.message?.text?.takeIf { it.isNotBlank() }
                    ?: "Unable to load warehouses."
            )
        }
    }

    private fun mapInventoryResponse(
        payload: BaseApiModel<List<InventoryDto>>
    ): ApiResult<InventoryPageResult> {
        val data = payload.data?.data.orEmpty()
        return if (payload.data?.success == true) {
            ApiResult.Success(
                InventoryPageResult(
                    items = data.map { it.toModel() },
                    totalCount = data.firstOrNull()?.totalCount ?: data.size
                )
            )
        } else {
            ApiResult.UnknownError(
                payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: payload.message?.text?.takeIf { it.isNotBlank() }
                    ?: "Unable to load inventory."
            )
        }
    }

    private fun mapInventoryTransactionsResponse(
        payload: BaseApiModel<List<InventoryTransactionDto>>
    ): ApiResult<InventoryTransactionsResult> {
        val data = payload.data?.data.orEmpty()
        return if (payload.data?.success == true) {
            ApiResult.Success(
                InventoryTransactionsResult(
                    transactions = data.map { it.toModel() },
                    totalCount = data.firstOrNull()?.totalCount ?: data.size
                )
            )
        } else {
            ApiResult.UnknownError(
                payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: payload.message?.text?.takeIf { it.isNotBlank() }
                    ?: "Unable to load inventory transactions."
            )
        }
    }

    private fun mapWarehouseReceiptsResponse(
        payload: BaseApiModel<List<WarehouseReceiptDto>>
    ): ApiResult<WarehouseReceiptsPageResult> {
        val data = payload.data?.data.orEmpty()
        return if (payload.data?.success == true) {
            ApiResult.Success(
                WarehouseReceiptsPageResult(
                    receipts = data.map { it.toModel() },
                    totalCount = data.firstOrNull()?.totalCount ?: data.size
                )
            )
        } else {
            ApiResult.UnknownError(
                payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: payload.message?.text?.takeIf { it.isNotBlank() }
                    ?: "Unable to load warehouse receipts."
            )
        }
    }

    private fun mapWarehouseReceiptDetailResponse(
        payload: BaseApiModel<WarehouseReceiptSingleDto>
    ): ApiResult<WarehouseReceiptDetailModel> {
        val detail = payload.data?.data
        val receipt = detail?.receipt
        return if (payload.data?.success == true && receipt != null) {
            ApiResult.Success(
                WarehouseReceiptDetailModel(
                    receipt = receipt.toModel(),
                    lines = detail.lines.orEmpty().map { it.toModel() }
                )
            )
        } else {
            ApiResult.UnknownError(
                payload.data?.message?.takeIf { it.isNotBlank() }
                    ?: payload.message?.text?.takeIf { it.isNotBlank() }
                    ?: "Unable to load receipt details."
            )
        }
    }

    private fun WarehouseDto.toModel(): WarehouseModel {
        return WarehouseModel(
            id = id.orEmpty(),
            code = code.orEmpty().ifBlank { "-" },
            name = name.orEmpty().ifBlank { "-" },
            addressLine1 = addressLine1.orEmpty(),
            addressLine2 = addressLine2.orEmpty(),
            city = city.orEmpty().ifBlank { "-" },
            state = state.orEmpty().ifBlank { "-" },
            postalCode = postalCode.orEmpty(),
            country = country.orEmpty().ifBlank { "-" },
            isActive = isActive == true || status == ACTIVE_STATUS,
            createdDate = createdDate.orEmpty()
        )
    }

    private fun WarehouseLocationDto.toModel(): WarehouseLocationModel {
        return WarehouseLocationModel(
            id = id.orEmpty(),
            warehouseId = warehouseId.orEmpty(),
            warehouseName = warehouseName.orEmpty().ifBlank { "-" },
            levelType = levelType ?: 0,
            levelTypeName = levelTypeName.orEmpty().ifBlank { levelType.toLocationLevelName() },
            code = code.orEmpty().ifBlank { "-" },
            name = name.orEmpty().ifBlank { "-" },
            path = path.orEmpty().ifBlank { "-" },
            isDefaultArea = isDefaultArea == true,
            isActive = isActive == true
        )
    }

    private fun Int?.toLocationLevelName(): String {
        return when (this) {
            1 -> "Area"
            2 -> "Zone"
            3 -> "Location"
            4 -> "Rack"
            5 -> "Bin"
            else -> "-"
        }
    }

    private fun WarehouseLocationDdlDto.toOptionModel(): WarehouseLocationOptionModel {
        return WarehouseLocationOptionModel(
            id = id.orEmpty(),
            name = name.orEmpty().ifBlank { "-" },
            path = path.orEmpty().ifBlank { "-" },
            levelType = levelType ?: 0
        )
    }

    private fun WarehouseDdlDto.toOptionModel(): WarehouseOptionModel {
        return WarehouseOptionModel(
            id = id.orEmpty(),
            code = code.orEmpty().ifBlank { "-" },
            name = name.orEmpty().ifBlank { "-" }
        )
    }

    private fun PurchaseOrderDdlDto.toOptionModel(): PurchaseOrderOptionModel {
        return PurchaseOrderOptionModel(
            id = id.orEmpty(),
            number = poNumber.orEmpty().ifBlank { "-" }
        )
    }

    private fun InventoryDto.toModel(): InventoryModel {
        return InventoryModel(
            id = id.orEmpty(),
            productName = productName.orEmpty().ifBlank { "-" },
            sourcePurchaseOrderId = sourcePurchaseOrderId.orEmpty(),
            sourcePurchaseOrderNumber = sourcePurchaseOrderNumber
                .orEmpty()
                .ifBlank { poNumber.orEmpty() }
                .ifBlank { "-" },
            warehouseId = warehouseId.orEmpty(),
            warehouseName = warehouseName.orEmpty().ifBlank { "-" },
            locationId = locationId.orEmpty(),
            locationPath = locationPath.orEmpty().ifBlank { "-" },
            ownershipType = ownershipType,
            purpose = purpose,
            inventoryStatus = inventoryStatus,
            isSerialTracked = isSerialTracked == true,
            quantityOnHand = quantityOnHand ?: 0.0,
            quantityReserved = quantityReserved ?: 0.0,
            quantityAvailable = quantityAvailable ?: 0.0,
            balanceStatus = balanceStatus.orEmpty()
                .ifBlank { inventoryStatus.toInventoryStatusName() }
                .toDisplayWords(),
            createdDate = createdDate.orEmpty()
        )
    }

    private fun InventoryTransactionDto.toModel(): InventoryTransactionModel {
        return InventoryTransactionModel(
            id = id.orEmpty(),
            transactionTypeName = transactionTypeName.orEmpty()
                .ifBlank { transactionType.toInventoryTransactionTypeName() }
                .toDisplayWords(),
            quantity = quantity ?: 0.0,
            fromLocationPath = fromLocationPath.orEmpty().ifBlank { "-" },
            toLocationPath = toLocationPath.orEmpty().ifBlank { "-" },
            referenceType = referenceType.orEmpty().ifBlank { "-" }.toDisplayWords(),
            notes = notes.orEmpty(),
            createdDate = createdDate.orEmpty()
        )
    }

    private fun WarehouseReceiptDto.toModel(): WarehouseReceiptModel {
        return WarehouseReceiptModel(
            id = id.orEmpty(),
            receiptNumber = receiptNumber.orEmpty().ifBlank { "-" },
            receiptType = receiptType,
            receiptTypeName = receiptType.toReceiptTypeName(),
            purchaseOrderId = purchaseOrderId.orEmpty(),
            poNumber = poNumber.orEmpty().ifBlank { "-" },
            warehouseId = warehouseId.orEmpty(),
            warehouseName = warehouseName.orEmpty().ifBlank { "-" },
            receiptStatus = receiptStatus,
            receiptStatusName = receiptStatus.toReceiptStatusName(),
            receivedDate = receivedDate.orEmpty().ifBlank { "-" },
            createdDate = createdDate.orEmpty().ifBlank { "-" }
        )
    }

    private fun WarehouseReceiptLineDto.toModel(): WarehouseReceiptLineModel {
        val remaining = quantityRemainingToPutaway ?: 0.0
        val canReceiveValue = canReceive == true
        val canInspectValue = canInspect == true
        val canPutawayValue = canPutaway == true
        return WarehouseReceiptLineModel(
            id = id.orEmpty(),
            receiptId = receiptId.orEmpty(),
            productId = productId.orEmpty(),
            productName = productName.orEmpty().ifBlank { "-" },
            productCode = "-",
            uom = "-",
            expectedQuantity = expectedQuantity ?: 0.0,
            receivedQuantity = receivedQuantity ?: 0.0,
            quantityPutaway = quantityPutaway ?: 0.0,
            quantityRemainingToPutaway = remaining,
            inspectionStatus = inspectionStatus,
            inspectionStatusName = inspectionStatus.toInspectionStatusName(),
            isSerialTracked = isSerialTracked == true,
            canReceive = canReceiveValue,
            canInspect = canInspectValue,
            canPutaway = canPutawayValue,
            actionStatusName = when {
                canReceiveValue -> "Receive"
                canInspectValue -> "Inspect"
                canPutawayValue -> "Putaway"
                remaining <= 0.0 -> "Completed"
                else -> "-"
            }
        )
    }

    private fun Int?.toInventoryStatusName(): String {
        return when (this) {
            1 -> "Available"
            2 -> "Reserved"
            3 -> "Damaged"
            4 -> "Quarantine"
            else -> "-"
        }
    }

    private fun Int?.toInventoryTransactionTypeName(): String {
        return when (this) {
            1 -> "Receipt"
            2 -> "Putaway"
            4 -> "Reservation"
            10 -> "Move To Staging"
            else -> "-"
        }
    }

    private fun Int?.toReceiptTypeName(): String {
        return when (this) {
            1 -> "Purchase Order"
            2 -> "Customer Provided"
            else -> "-"
        }
    }

    private fun Int?.toReceiptStatusName(): String {
        return when (this) {
            1 -> "Expected"
            2 -> "Receiving"
            3 -> "Inspection"
            4 -> "Accepted"
            5 -> "Rejected"
            6 -> "Putaway Pending"
            7 -> "Completed"
            else -> "-"
        }
    }

    private fun Int?.toInspectionStatusName(): String {
        return when (this) {
            1 -> "Pending"
            2 -> "Accepted"
            3 -> "Rejected"
            else -> "-"
        }
    }

    private fun String.toDisplayWords(): String {
        return replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replace(Regex("(?<=[A-Za-z])(?=\\d)"), " ")
            .trim()
    }

    companion object {
        const val PAGE_SIZE = 10
        private const val ACTIVE_STATUS = 1
    }
}
