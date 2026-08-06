package com.axelliant.hris.features.inventory.products.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.inventory.products.data.remote.ProductsApiService
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductDetailResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductListResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSearchFilters
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSearchRequest
import com.google.gson.JsonElement
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProductsRepository @Inject constructor(
    private val apiService: ProductsApiService,
    private val safeApiExecutor: SafeApiExecutor
) {
    private var cachedAdvancedProductFilters: ApiResult<JsonElement>? = null

    suspend fun getProducts(
        search: String? = null,
        status: Int? = null,
        filters: ProductSearchFilters = ProductSearchFilters(),
        page: Int = 1,
        limit: Int = 20,
        from: Int = (page - 1).coerceAtLeast(0) * limit
    ): ApiResult<ProductListResponse> = withContext(Dispatchers.IO) {
        safeApiExecutor.execute {
            apiService.searchProducts(
                ProductSearchRequest.create(
                    size = limit,
                    from = from,
                    search = search,
                    status = status,
                    filters = filters
                )
            )
        }
    }

    suspend fun getAdvancedProductFilters(): ApiResult<JsonElement> {
        cachedAdvancedProductFilters?.let { return it }
        return withContext(Dispatchers.IO) {
            safeApiExecutor.execute { apiService.getAdvancedProductFilters() }
        }
            .also { result -> if (result is ApiResult.Success) cachedAdvancedProductFilters = result }
    }

    suspend fun getProductDetail(id: String): ApiResult<ProductDetailResponse> {
        return withContext(Dispatchers.IO) {
            safeApiExecutor.execute { apiService.getProductDetail(id) }
        }
    }
}
