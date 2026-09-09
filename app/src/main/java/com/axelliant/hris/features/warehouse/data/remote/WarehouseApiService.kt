package com.axelliant.hris.features.warehouse.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseLocationRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetInventoryRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetInventoryTransactionsRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetWarehouseReceiptsRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.GetWarehousesRequest
import com.axelliant.hris.features.warehouse.data.remote.dto.InventoryDto
import com.axelliant.hris.features.warehouse.data.remote.dto.InventoryTransactionDto
import com.axelliant.hris.features.warehouse.data.remote.dto.PurchaseOrderDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseReceiptDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseReceiptSingleDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseLocationDdlDto
import com.axelliant.hris.features.warehouse.data.remote.dto.WarehouseLocationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WarehouseApiService {
    @POST("Warehouse/GetAll")
    suspend fun getWarehouses(
        @Body request: GetWarehousesRequest
    ): Response<BaseApiModel<List<WarehouseDto>>>

    @POST("Warehouse/Add")
    suspend fun addWarehouse(
        @Body request: AddWarehouseRequest
    ): Response<BaseApiModel<WarehouseDto>>

    @GET("Warehouse/GetLocationTree")
    suspend fun getLocationTree(
        @Query("warehouseId") warehouseId: String
    ): Response<BaseApiModel<List<WarehouseLocationDto>>>

    @GET("Warehouse/GetLocationDDL")
    suspend fun getLocationDdl(
        @Query("warehouseId") warehouseId: String
    ): Response<BaseApiModel<List<WarehouseLocationDdlDto>>>

    @POST("Warehouse/AddLocation")
    suspend fun addLocation(
        @Body request: AddWarehouseLocationRequest
    ): Response<BaseApiModel<WarehouseLocationDto>>

    @GET("Warehouse/GetWarehouseDDL")
    suspend fun getWarehouseDdl(
        @Query("search") search: String = ""
    ): Response<BaseApiModel<List<WarehouseDdlDto>>>

    @GET("WarehouseReceipt/GetPurchaseOrderDDL")
    suspend fun getPurchaseOrderDdl(
        @Query("isListing") isListing: Boolean = true
    ): Response<BaseApiModel<List<PurchaseOrderDdlDto>>>

    @POST("Inventory/GetAll")
    suspend fun getInventory(
        @Body request: GetInventoryRequest
    ): Response<BaseApiModel<List<InventoryDto>>>

    @POST("Inventory/GetTransactions")
    suspend fun getInventoryTransactions(
        @Body request: GetInventoryTransactionsRequest
    ): Response<BaseApiModel<List<InventoryTransactionDto>>>

    @POST("WarehouseReceipt/GetAll")
    suspend fun getWarehouseReceipts(
        @Body request: GetWarehouseReceiptsRequest
    ): Response<BaseApiModel<List<WarehouseReceiptDto>>>

    @GET("WarehouseReceipt/GetSingle")
    suspend fun getWarehouseReceipt(
        @Query("id") receiptId: String
    ): Response<BaseApiModel<WarehouseReceiptSingleDto>>
}
