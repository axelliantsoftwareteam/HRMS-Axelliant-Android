package com.axelliant.hris.features.inventory.assets.data.remote

import com.axelliant.hris.features.inventory.assets.data.remote.dto.AssetDetailResponse
import com.axelliant.hris.features.inventory.assets.data.remote.dto.AssetListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AssetsApiService {
    @GET("assets")
    suspend fun getAssets(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("assignedTo") assignedTo: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<AssetListResponse>

    @GET("assets/{id}")
    suspend fun getAssetDetail(@Path("id") id: String): Response<AssetDetailResponse>
}
