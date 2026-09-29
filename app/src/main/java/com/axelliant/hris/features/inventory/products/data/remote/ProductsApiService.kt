package com.axelliant.hris.features.inventory.products.data.remote

import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductDetailResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductListResponse
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface ProductsApiService {
    @POST("ElasticSearch/AdvanceProductsSearch")
    suspend fun searchProducts(@Body request: JsonElement): Response<ProductListResponse>

    @GET("ElasticSearch/AdvanceProductsSearchFilter")
    suspend fun getAdvancedProductFilters(): Response<JsonElement>

    @GET("products/{id}")
    suspend fun getProductDetail(@Path("id") id: String): Response<ProductDetailResponse>
}
