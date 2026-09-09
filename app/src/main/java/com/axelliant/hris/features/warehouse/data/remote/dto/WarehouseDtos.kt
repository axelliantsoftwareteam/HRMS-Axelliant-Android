package com.axelliant.hris.features.warehouse.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GetWarehousesRequest(
    @SerializedName("start")
    val start: Int = 0,
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("sort")
    val sort: String = "",
    @SerializedName("order")
    val order: String = "",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = ""
)

data class GetInventoryRequest(
    @SerializedName("start")
    val start: Int = 0,
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("sort")
    val sort: String = "",
    @SerializedName("order")
    val order: String = "",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = "",
    @SerializedName("warehouseId")
    val warehouseId: String? = null,
    @SerializedName("locationId")
    val locationId: String? = null,
    @SerializedName("purchaseOrderId")
    val purchaseOrderId: String? = null,
    @SerializedName("ownershipType")
    val ownershipType: Int? = null,
    @SerializedName("purpose")
    val purpose: Int? = null,
    @SerializedName("inventoryStatus")
    val inventoryStatus: Int? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("includeDepleted")
    val includeDepleted: Boolean = true
)

data class GetInventoryTransactionsRequest(
    @SerializedName("start")
    val start: Int = 0,
    @SerializedName("limit")
    val limit: Int = 50,
    @SerializedName("sort")
    val sort: String = "CreatedOn",
    @SerializedName("order")
    val order: String = "desc",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = "",
    @SerializedName("inventoryId")
    val inventoryId: String
)

data class GetWarehouseReceiptsRequest(
    @SerializedName("start")
    val start: Int = 0,
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("sort")
    val sort: String = "",
    @SerializedName("order")
    val order: String = "",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = "",
    @SerializedName("warehouseId")
    val warehouseId: String? = null,
    @SerializedName("purchaseOrderId")
    val purchaseOrderId: String? = null,
    @SerializedName("receiptStatus")
    val receiptStatus: Int? = null,
    @SerializedName("receiptType")
    val receiptType: Int? = null
)

data class AddWarehouseRequest(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("addressLine1")
    val addressLine1: String = "",
    @SerializedName("addressLine2")
    val addressLine2: String = "",
    @SerializedName("city")
    val city: String = "",
    @SerializedName("state")
    val state: String = "",
    @SerializedName("postalCode")
    val postalCode: String = "",
    @SerializedName("country")
    val country: String = "",
    @SerializedName("isActive")
    val isActive: Boolean = true
)

data class AddWarehouseLocationRequest(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("warehouseId")
    val warehouseId: String,
    @SerializedName("parentId")
    val parentId: String? = null,
    @SerializedName("levelType")
    val levelType: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("isActive")
    val isActive: Boolean = true
)

data class WarehouseDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("addressLine1")
    val addressLine1: String? = null,
    @SerializedName("addressLine2")
    val addressLine2: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("postalCode")
    val postalCode: String? = null,
    @SerializedName("country")
    val country: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("serialNo")
    val serialNo: Int? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class WarehouseDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null
)

data class PurchaseOrderDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("poNumber")
    val poNumber: String? = null
)

data class WarehouseLocationDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("warehouseId")
    val warehouseId: String? = null,
    @SerializedName("levelType")
    val levelType: Int? = null,
    @SerializedName("levelTypeName")
    val levelTypeName: String? = null,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("isDefaultArea")
    val isDefaultArea: Boolean? = null,
    @SerializedName("isActive")
    val isActive: Boolean? = null,
    @SerializedName("warehouseName")
    val warehouseName: String? = null
)

data class WarehouseLocationDdlDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("levelType")
    val levelType: Int? = null
)

data class InventoryDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("productName")
    val productName: String? = null,
    @SerializedName("sourcePurchaseOrderId")
    val sourcePurchaseOrderId: String? = null,
    @SerializedName("sourcePurchaseOrderNumber")
    val sourcePurchaseOrderNumber: String? = null,
    @SerializedName("poNumber")
    val poNumber: String? = null,
    @SerializedName("warehouseId")
    val warehouseId: String? = null,
    @SerializedName("warehouseName")
    val warehouseName: String? = null,
    @SerializedName("locationId")
    val locationId: String? = null,
    @SerializedName("locationPath")
    val locationPath: String? = null,
    @SerializedName("ownershipType")
    val ownershipType: Int? = null,
    @SerializedName("purpose")
    val purpose: Int? = null,
    @SerializedName("inventoryStatus")
    val inventoryStatus: Int? = null,
    @SerializedName("isSerialTracked")
    val isSerialTracked: Boolean? = null,
    @SerializedName("quantityOnHand")
    val quantityOnHand: Double? = null,
    @SerializedName("quantityReserved")
    val quantityReserved: Double? = null,
    @SerializedName("quantityAvailable")
    val quantityAvailable: Double? = null,
    @SerializedName("balanceStatus")
    val balanceStatus: String? = null,
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class InventoryTransactionDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("transactionType")
    val transactionType: Int? = null,
    @SerializedName("transactionTypeName")
    val transactionTypeName: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("inventoryId")
    val inventoryId: String? = null,
    @SerializedName("quantity")
    val quantity: Double? = null,
    @SerializedName("fromLocationId")
    val fromLocationId: String? = null,
    @SerializedName("fromLocationPath")
    val fromLocationPath: String? = null,
    @SerializedName("toLocationId")
    val toLocationId: String? = null,
    @SerializedName("toLocationPath")
    val toLocationPath: String? = null,
    @SerializedName("referenceType")
    val referenceType: String? = null,
    @SerializedName("referenceId")
    val referenceId: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("serialNo")
    val serialNo: Int? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class WarehouseReceiptDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("receiptNumber")
    val receiptNumber: String? = null,
    @SerializedName("receiptType")
    val receiptType: Int? = null,
    @SerializedName("purchaseOrderId")
    val purchaseOrderId: String? = null,
    @SerializedName("poNumber")
    val poNumber: String? = null,
    @SerializedName("warehouseId")
    val warehouseId: String? = null,
    @SerializedName("warehouseName")
    val warehouseName: String? = null,
    @SerializedName("receiptStatus")
    val receiptStatus: Int? = null,
    @SerializedName("reservationStatus")
    val reservationStatus: Int? = null,
    @SerializedName("receivedDate")
    val receivedDate: String? = null,
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("serialNo")
    val serialNo: Int? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("createdOn")
    val createdOn: Long? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null
)

data class WarehouseReceiptSingleDto(
    @SerializedName("receipt")
    val receipt: WarehouseReceiptDto? = null,
    @SerializedName("lines")
    val lines: List<WarehouseReceiptLineDto>? = null
)

data class WarehouseReceiptLineDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("receiptId")
    val receiptId: String? = null,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("productName")
    val productName: String? = null,
    @SerializedName("expectedQuantity")
    val expectedQuantity: Double? = null,
    @SerializedName("receivedQuantity")
    val receivedQuantity: Double? = null,
    @SerializedName("inventoryId")
    val inventoryId: String? = null,
    @SerializedName("isSerialTracked")
    val isSerialTracked: Boolean? = null,
    @SerializedName("quantityPutaway")
    val quantityPutaway: Double? = null,
    @SerializedName("quantityRemainingToPutaway")
    val quantityRemainingToPutaway: Double? = null,
    @SerializedName("inspectionStatus")
    val inspectionStatus: Int? = null,
    @SerializedName("serialNumbers")
    val serialNumbers: List<String>? = null,
    @SerializedName("canReceive")
    val canReceive: Boolean? = null,
    @SerializedName("canInspect")
    val canInspect: Boolean? = null,
    @SerializedName("canPutaway")
    val canPutaway: Boolean? = null
)
