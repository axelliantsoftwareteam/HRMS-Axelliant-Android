package com.axelliant.hris.features.purchaseorders.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPosPurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderRequest
import com.axelliant.hris.features.purchaseorders.data.remote.dto.AddEditPurchaseOrderResponseDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.CompanyAddressesDto
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetPurchaseOrdersRequest
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetPurchaseOrdersResponse
import com.axelliant.hris.features.purchaseorders.data.remote.dto.GetSinglePurchaseOrderResponse
import com.axelliant.hris.features.purchaseorders.data.remote.dto.VendorDdlDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PurchaseOrderApiService {

    @POST("PurchaseOrder/GetAll")
    suspend fun getAllPurchaseOrders(
        @Body request: GetPurchaseOrdersRequest
    ): Response<BaseApiModel<GetPurchaseOrdersResponse>>

    @GET("PurchaseOrder/GetSingle")
    suspend fun getSinglePurchaseOrder(
        @Query("id") id: String
    ): Response<BaseApiModel<GetSinglePurchaseOrderResponse>>

    @GET("Vendor/GetAllForDDL")
    suspend fun getVendorsForDdl(
        @Query("search") search: String = ""
    ): Response<BaseApiModel<List<VendorDdlDto>>>

    @GET("CompanyAddress/GetAllForDDL")
    suspend fun getCompanyAddressesForDdl(): Response<BaseApiModel<CompanyAddressesDto>>

    @POST("PurchaseOrder/AddEdit")
    suspend fun addEditPurchaseOrder(
        @Body request: AddEditPurchaseOrderRequest
    ): Response<BaseApiModel<List<AddEditPurchaseOrderResponseDto>>>

    @POST("PurchaseOrder/AddEdit")
    suspend fun addEditPosPurchaseOrder(
        @Body request: AddEditPosPurchaseOrderRequest
    ): Response<BaseApiModel<List<AddEditPurchaseOrderResponseDto>>>
}
