package com.axelliant.hris.features.saleorders.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.saleorders.data.remote.dto.AddEditSaleOrderRequest
import com.axelliant.hris.features.saleorders.data.remote.dto.GetSaleOrdersRequest
import com.axelliant.hris.features.saleorders.data.remote.dto.GetSaleOrdersResponse
import com.axelliant.hris.features.saleorders.data.remote.dto.SaleOrderSingleDto
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SaleOrderApiService {

    @POST("SalesOrder/GetAll")
    suspend fun getAllSaleOrders(
        @Body request: GetSaleOrdersRequest
    ): Response<BaseApiModel<GetSaleOrdersResponse>>

    @GET("SalesOrder/GetSingle")
    suspend fun getSingleSaleOrder(
        @Query("id") id: String
    ): Response<BaseApiModel<SaleOrderSingleDto>>

    @POST("SalesOrder/AddEdit")
    suspend fun addEditSaleOrder(
        @Body request: AddEditSaleOrderRequest
    ): Response<BaseApiModel<JsonElement>>

    @POST("SalesOrder/SubmitSaleOrder")
    suspend fun submitSaleOrder(
        @Query("saleOrderId") saleOrderId: String
    ): Response<BaseApiModel<Unit>>
}
