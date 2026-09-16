package com.axelliant.hris.features.inventory.assets.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.inventory.assets.data.remote.AssetsApiService
import com.axelliant.hris.features.inventory.assets.data.remote.dto.AssetDetailResponse
import com.axelliant.hris.features.inventory.assets.data.remote.dto.AssetListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetsRepository @Inject constructor(
    private val apiService: AssetsApiService,
    private val safeApiExecutor: SafeApiExecutor
) {
    suspend fun getAssets(
        search: String? = null,
        status: String? = null,
        assignedTo: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<AssetListResponse> {
        return safeApiExecutor.execute {
            apiService.getAssets(
                search = search,
                status = status,
                assignedTo = assignedTo,
                page = page,
                limit = limit
            )
        }
    }

    suspend fun getAssetDetail(id: String): ApiResult<AssetDetailResponse> {
        return safeApiExecutor.execute { apiService.getAssetDetail(id) }
    }
}
