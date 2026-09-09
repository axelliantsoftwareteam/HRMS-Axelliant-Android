package com.axelliant.hris.features.warehouse.domain.model

data class WarehouseModel(
    val id: String,
    val code: String,
    val name: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val isActive: Boolean,
    val createdDate: String
)

data class WarehousePageResult(
    val warehouses: List<WarehouseModel>,
    val totalCount: Int
)

data class WarehouseLocationModel(
    val id: String,
    val warehouseId: String,
    val warehouseName: String,
    val levelType: Int,
    val levelTypeName: String,
    val code: String,
    val name: String,
    val path: String,
    val isDefaultArea: Boolean,
    val isActive: Boolean
)

data class WarehouseLocationOptionModel(
    val id: String,
    val name: String,
    val path: String,
    val levelType: Int
)

data class WarehouseOptionModel(
    val id: String,
    val code: String,
    val name: String
)

data class PurchaseOrderOptionModel(
    val id: String,
    val number: String
)

data class InventoryModel(
    val id: String,
    val productName: String,
    val sourcePurchaseOrderId: String,
    val sourcePurchaseOrderNumber: String,
    val warehouseId: String,
    val warehouseName: String,
    val locationId: String,
    val locationPath: String,
    val ownershipType: Int?,
    val purpose: Int?,
    val inventoryStatus: Int?,
    val isSerialTracked: Boolean,
    val quantityOnHand: Double,
    val quantityReserved: Double,
    val quantityAvailable: Double,
    val balanceStatus: String,
    val createdDate: String
)

data class InventoryPageResult(
    val items: List<InventoryModel>,
    val totalCount: Int
)

data class InventoryTransactionModel(
    val id: String,
    val transactionTypeName: String,
    val quantity: Double,
    val fromLocationPath: String,
    val toLocationPath: String,
    val referenceType: String,
    val notes: String,
    val createdDate: String
)

data class InventoryTransactionsResult(
    val transactions: List<InventoryTransactionModel>,
    val totalCount: Int
)

data class WarehouseReceiptModel(
    val id: String,
    val receiptNumber: String,
    val receiptType: Int?,
    val receiptTypeName: String,
    val purchaseOrderId: String,
    val poNumber: String,
    val warehouseId: String,
    val warehouseName: String,
    val receiptStatus: Int?,
    val receiptStatusName: String,
    val receivedDate: String,
    val createdDate: String
)

data class WarehouseReceiptsPageResult(
    val receipts: List<WarehouseReceiptModel>,
    val totalCount: Int
)

data class WarehouseReceiptDetailModel(
    val receipt: WarehouseReceiptModel,
    val lines: List<WarehouseReceiptLineModel>
)

data class WarehouseReceiptLineModel(
    val id: String,
    val receiptId: String,
    val productId: String,
    val productName: String,
    val productCode: String,
    val uom: String,
    val expectedQuantity: Double,
    val receivedQuantity: Double,
    val quantityPutaway: Double,
    val quantityRemainingToPutaway: Double,
    val inspectionStatus: Int?,
    val inspectionStatusName: String,
    val isSerialTracked: Boolean,
    val canReceive: Boolean,
    val canInspect: Boolean,
    val canPutaway: Boolean,
    val actionStatusName: String
)
