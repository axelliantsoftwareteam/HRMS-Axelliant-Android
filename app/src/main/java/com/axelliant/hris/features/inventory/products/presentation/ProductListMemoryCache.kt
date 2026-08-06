package com.axelliant.hris.features.inventory.products.presentation

import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSearchFilters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductListMemoryCache @Inject constructor() {
    private val snapshots = LinkedHashMap<ProductListCacheKey, ProductListSnapshot>(
        MAX_ENTRIES,
        LOAD_FACTOR,
        true
    )

    @Synchronized
    fun get(key: ProductListCacheKey): ProductListSnapshot? = snapshots[key]

    @Synchronized
    fun put(snapshot: ProductListSnapshot) {
        snapshots[snapshot.key] = snapshot
        while (snapshots.size > MAX_ENTRIES) {
            val oldestKey = snapshots.keys.firstOrNull() ?: return
            snapshots.remove(oldestKey)
        }
    }

    private companion object {
        const val MAX_ENTRIES = 4
        const val LOAD_FACTOR = 0.75f
    }
}

data class ProductListCacheKey(
    val searchQuery: String,
    val statusFilter: ProductStatusFilter,
    val filters: ProductSearchFilters
)

data class ProductListSnapshot(
    val key: ProductListCacheKey,
    val products: List<ProductListItemUi>,
    val totalProducts: Int,
    val nextFrom: Int,
    val tabCounts: ProductTabCountsUiModel
)
