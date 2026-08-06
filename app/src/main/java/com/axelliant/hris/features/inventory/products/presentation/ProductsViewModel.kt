package com.axelliant.hris.features.inventory.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.core.ui.toUiState
import com.axelliant.hris.features.inventory.products.data.ProductsRepository
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductDetailResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductListResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSearchFilters
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductSourceResponse
import com.axelliant.hris.features.inventory.products.data.remote.dto.ProductVendorResponse
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.DynamicSpecFilter
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.StaticCategoryFilterRepository
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val staticCategoryFilterRepository: StaticCategoryFilterRepository,
    private val productListMemoryCache: ProductListMemoryCache
) : ViewModel() {
    private val _productsState = MutableStateFlow<UiState<ProductListUiModel>>(UiState.Idle)
    val productsState = _productsState.asStateFlow()
    private val _tabCountsState = MutableStateFlow(ProductTabCountsUiModel.empty())
    val tabCountsState = _tabCountsState.asStateFlow()
    private val _categoryFiltersState = MutableStateFlow<UiState<List<FilterOptionUi>>>(UiState.Empty)
    val categoryFiltersState = _categoryFiltersState.asStateFlow()
    private val _manufacturerFiltersState = MutableStateFlow<UiState<List<FilterOptionUi>>>(UiState.Idle)
    val manufacturerFiltersState = _manufacturerFiltersState.asStateFlow()
    private val _vendorFiltersState = MutableStateFlow<UiState<List<FilterOptionUi>>>(UiState.Idle)
    val vendorFiltersState = _vendorFiltersState.asStateFlow()
    private val _attributeTagFiltersState = MutableStateFlow<UiState<List<FilterOptionUi>>>(UiState.Idle)
    val attributeTagFiltersState = _attributeTagFiltersState.asStateFlow()
    private val loadedProducts = mutableListOf<ProductListItemUi>()
    private var totalProducts = 0
    private var nextFrom = 0
    private var isPageLoading = false
    private var activeSearchQuery = ""
    private var activeStatusFilter = ProductStatusFilter.All
    private var activeFilters = ProductSearchFilters()
    private var productsLoadJob: Job? = null

    fun loadProducts(
        searchQuery: String? = activeSearchQuery,
        statusFilter: ProductStatusFilter = activeStatusFilter,
        filters: ProductSearchFilters = activeFilters
    ) {
        productsLoadJob?.cancel()
        productsLoadJob = viewModelScope.launch {
            isPageLoading = false
            activeSearchQuery = searchQuery.orEmpty().trim()
            activeStatusFilter = statusFilter
            activeFilters = filters
            loadedProducts.clear()
            totalProducts = 0
            nextFrom = 0
            _productsState.value = UiState.Loading
            loadPage(from = 0, append = false)
        }
    }

    fun loadProductsIfNeeded() {
        if (_productsState.value !is UiState.Idle) return

        val cacheKey = currentCacheKey()
        val cachedSnapshot = productListMemoryCache.get(cacheKey)
        if (cachedSnapshot == null) {
            loadProducts()
            return
        }

        hydrateProducts(cachedSnapshot)
        refreshCachedProducts(cachedSnapshot)
    }

    fun loadCommonProductFilters() {
        val alreadyLoaded = _manufacturerFiltersState.value is UiState.Success &&
            _vendorFiltersState.value is UiState.Success &&
            _attributeTagFiltersState.value is UiState.Success
        val isLoading = _manufacturerFiltersState.value is UiState.Loading ||
            _vendorFiltersState.value is UiState.Loading ||
            _attributeTagFiltersState.value is UiState.Loading
        if (alreadyLoaded || isLoading) return

        viewModelScope.launch {
            _manufacturerFiltersState.value = UiState.Loading
            _vendorFiltersState.value = UiState.Loading
            _attributeTagFiltersState.value = UiState.Loading

            val commonFilters = staticCategoryFilterRepository.getCommonFilters()
            _manufacturerFiltersState.value = commonFilters
                .optionsForKey(COMMON_MANUFACTURER_KEY)
                .toLookupUiState()
            _vendorFiltersState.value = commonFilters
                .optionsForKey(COMMON_VENDOR_KEY)
                .toLookupUiState()
            _attributeTagFiltersState.value = commonFilters
                .optionsForKey(COMMON_ATTRIBUTE_TAGS_KEY)
                .toLookupUiState()
        }
    }

    fun loadNextPage() {
        if (isPageLoading || loadedProducts.isEmpty() || loadedProducts.size >= totalProducts) return

        productsLoadJob = viewModelScope.launch {
            _productsState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(from = nextFrom, append = true)
        }
    }

    private suspend fun loadPage(
        from: Int,
        append: Boolean,
        pageSize: Int = PRODUCT_PAGE_SIZE,
        keepExistingOnError: Boolean = false
    ) {
        isPageLoading = true
        _productsState.value = when (
            val result = productsRepository.getProducts(
                search = activeSearchQuery.takeIf { it.isNotEmpty() },
                status = activeStatusFilter.apiStatus,
                filters = activeFilters,
                limit = pageSize,
                from = from
            )
        ) {
            is ApiResult.Success -> {
                val (page, tabCounts) = withContext(Dispatchers.Default) {
                    result.data.toProductListUiModel() to result.data.toProductTabCountsUiModel()
                }
                _tabCountsState.value = tabCounts
                totalProducts = page.total
                if (!append) loadedProducts.clear()
                loadedProducts.addAll(page.products)
                nextFrom = loadedProducts.size

                saveCurrentSnapshot()
                currentProductsState(isAppendUpdate = append)
            }

            ApiResult.Empty -> {
                if (!append) {
                    loadedProducts.clear()
                    totalProducts = 0
                    nextFrom = 0
                }
                saveCurrentSnapshot()
                currentProductsState(isAppendUpdate = append)
            }

            is ApiResult.HttpError -> loadErrorState(result.message, keepExistingOnError)
            is ApiResult.NetworkError -> loadErrorState(result.message, keepExistingOnError)
            is ApiResult.UnknownError -> loadErrorState(result.message, keepExistingOnError)
            ApiResult.Unauthorized -> UiState.Unauthorized
        }
        isPageLoading = false
    }

    private fun currentProductsState(isAppendUpdate: Boolean): UiState<ProductListUiModel> {
        return if (loadedProducts.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(
                currentUiModel(
                    isLoadingNextPage = false,
                    isAppendUpdate = isAppendUpdate
                )
            )
        }
    }

    private fun loadErrorState(
        message: String,
        keepExistingOnError: Boolean
    ): UiState<ProductListUiModel> {
        return if (keepExistingOnError && loadedProducts.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message)
        }
    }

    private fun hydrateProducts(snapshot: ProductListSnapshot) {
        activeSearchQuery = snapshot.key.searchQuery
        activeStatusFilter = snapshot.key.statusFilter
        activeFilters = snapshot.key.filters
        loadedProducts.clear()
        loadedProducts.addAll(snapshot.products)
        totalProducts = snapshot.totalProducts
        nextFrom = snapshot.nextFrom
        _tabCountsState.value = snapshot.tabCounts
        _productsState.value = currentProductsState(isAppendUpdate = false)
    }

    private fun refreshCachedProducts(snapshot: ProductListSnapshot) {
        productsLoadJob?.cancel()
        productsLoadJob = viewModelScope.launch {
            isPageLoading = false
            loadPage(
                from = 0,
                append = false,
                pageSize = PRODUCT_PAGE_SIZE,
                keepExistingOnError = true
            )
        }
    }

    private fun saveCurrentSnapshot() {
        val cachedProducts = loadedProducts.take(PRODUCT_CACHE_PRODUCT_LIMIT)
        productListMemoryCache.put(
            ProductListSnapshot(
                key = currentCacheKey(),
                products = cachedProducts,
                totalProducts = totalProducts,
                nextFrom = cachedProducts.size,
                tabCounts = _tabCountsState.value
            )
        )
    }

    private fun currentCacheKey(
        searchQuery: String = activeSearchQuery,
        statusFilter: ProductStatusFilter = activeStatusFilter,
        filters: ProductSearchFilters = activeFilters
    ): ProductListCacheKey {
        return ProductListCacheKey(
            searchQuery = searchQuery.trim(),
            statusFilter = statusFilter,
            filters = filters
        )
    }

    private fun currentUiModel(
        isLoadingNextPage: Boolean,
        isAppendUpdate: Boolean = false
    ): ProductListUiModel {
        return ProductListUiModel(
            total = totalProducts,
            products = loadedProducts.toList(),
            isLoadingNextPage = isLoadingNextPage,
            isAppendUpdate = isAppendUpdate
        )
    }

    private fun ProductListResponse.toProductListUiModel(): ProductListUiModel {
        val sourceProducts = products
            .takeIf { it.isNotEmpty() || hits == null }
            ?: hits?.hits.orEmpty().mapNotNull { it.source }

        val productItems = sourceProducts
            .map { it.toProductListItemUi() }

        return ProductListUiModel(
            total = total ?: hits?.total?.value ?: productItems.size,
            products = productItems
        )
    }

    private fun ProductListResponse.toProductTabCountsUiModel(): ProductTabCountsUiModel {
        return ProductTabCountsUiModel(
            all = (total ?: hits?.total?.value)?.toString() ?: COUNT_ERROR,
            active = activeCount?.toString() ?: COUNT_ERROR,
            inactive = inactiveCount?.toString() ?: COUNT_ERROR
        )
    }

    private fun ProductSourceResponse.toProductListItemUi(): ProductListItemUi {
        val vendor = vendorInfo.orEmpty().firstOrNull()
        val price = vendor?.listPrice ?: listPrice ?: 0.0
        val status = if (status == ACTIVE_STATUS) ProductStatusFilter.Active else ProductStatusFilter.Inactive
        val availability = if (stockStatus.equals(IN_STOCK_CODE, ignoreCase = true)) {
            ProductAvailability.InStock
        } else {
            ProductAvailability.OutOfStock
        }
        val thumbnail = buildThumbnailLabel()

        return ProductListItemUi(
            id = id.orEmpty(),
            name = name.orEmpty().ifBlank { "Unnamed Product" },
            sku = axePartNumber.orEmpty(),
            manufacturer = manufacturerName.orEmpty(),
            manufacturerPartNumber = manufacturerPartNumber.orEmpty(),
            vendor = vendor?.vendorName.orEmpty(),
            category = category?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
            description = description.orEmpty(),
            countryOfOrigin = countryOfOriginName ?: countryOfOrigin.orEmpty(),
            screenSize = screenSize.orEmpty(),
            dimensions = formatDimensions(width = width, height = height, length = length),
            unitOfMeasure = unitOfMeasure.orEmpty(),
            weight = weight?.formatPlainNumber().orEmpty(),
            warrantyPeriod = warrantyPeriod.orEmpty(),
            validityPeriod = validityPeriod.orEmpty(),
            vendors = vendorInfo.orEmpty().map { it.toVendorUi(availability) },
            listPrice = price.formatCurrency(),
            displayPrice = price.formatCurrency(),
            lastUpdate = updatedOn?.toString().orEmpty(),
            availability = availability,
            status = status,
            thumbnailLabel = thumbnail,
            brandThumbnail = thumbnail == CISCO_LABEL
        )
    }

    private fun ProductSourceResponse.buildThumbnailLabel(): String {
        val text = listOfNotNull(name, description, manufacturerName)
            .joinToString(" ")
            .uppercase(Locale.US)

        if (CISCO_LABEL in text) return CISCO_LABEL

        return manufacturerName
            ?.take(3)
            ?.uppercase(Locale.US)
            ?.ifBlank { null }
            ?: name.orEmpty().take(3).uppercase(Locale.US).ifBlank { "APP" }
    }

    private fun Double.formatCurrency(): String {
        return NumberFormat.getCurrencyInstance(Locale.US).format(this)
    }

    private fun Double.formatPlainNumber(): String {
        return if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", this)
        }
    }

    private fun formatDimensions(width: Double?, height: Double?, length: Double?): String {
        val values = listOfNotNull(width, height, length)
        if (values.isEmpty()) return ""
        return values.joinToString(" x ") { it.formatPlainNumber() }
    }

    private fun ProductVendorResponse.toVendorUi(productAvailability: ProductAvailability): ProductVendorUi {
        val active = isActive == true || activeFromVendor == true
        return ProductVendorUi(
            name = vendorName.orEmpty(),
            sku = listOf(vendorSku, vendorPartNumber)
                .mapNotNull { it?.takeIf(String::isNotBlank) }
                .joinToString(", "),
            availability = if ((quantity ?: 0) > 0 || productAvailability == ProductAvailability.InStock) {
                ProductAvailability.InStock.label
            } else {
                ProductAvailability.OutOfStock.label
            },
            unit = quantity?.toString().orEmpty(),
            costPrice = (cost ?: 0.0).formatCurrency(),
            listPrice = (listPrice ?: 0.0).formatCurrency(),
            status = if (active) "Active" else "Inactive"
        )
    }

    private fun List<FilterOptionUi>.toLookupUiState(): UiState<List<FilterOptionUi>> {
        return if (isEmpty()) UiState.Empty else UiState.Success(this)
    }

    private fun List<DynamicSpecFilter>.optionsForKey(key: String): List<FilterOptionUi> {
        return firstOrNull { filter -> filter.selectionKey.equals(key, ignoreCase = true) }
            ?.options
            .orEmpty()
            .mapNotNull { option ->
                option.name.trim().takeIf { it.isNotEmpty() }?.let { name ->
                    FilterOptionUi(id = name, name = name)
                }
            }
            .distinctBy { it.name.lowercase(Locale.US) }
    }

    private fun JsonElement.toFilterOptions(): List<FilterOptionUi> {
        return extractOptionElements().mapIndexedNotNull { index, element ->
            when {
                element.isJsonPrimitive -> {
                    val name = element.asString.orEmpty().trim()
                    name.takeIf { it.isNotEmpty() }?.let { FilterOptionUi(id = it, name = it) }
                }

                element.isJsonObject -> element.asJsonObject.toFilterOption(index)
                else -> null
            }
        }.distinctBy { it.name.lowercase(Locale.US) }
    }

    private fun JsonElement.extractOptionElements(): List<JsonElement> {
        if (isJsonArray) return asJsonArray.toList()
        if (!isJsonObject) return emptyList()

        val jsonObject = asJsonObject
        val candidateKeys = listOf(
            "data",
            "Data",
            "dataList",
            "DataList",
            "result",
            "Result",
            "items",
            "Items",
            "records",
            "Records",
            "rows",
            "Rows",
            "values",
            "Values"
        )
        candidateKeys.forEach { key ->
            jsonObject.get(key)?.extractOptionElements()?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        return jsonObject.entrySet()
            .asSequence()
            .mapNotNull { (_, value) -> value.extractOptionElements().takeIf { it.isNotEmpty() } }
            .firstOrNull()
            .orEmpty()
    }

    private fun JsonObject.toFilterOption(index: Int): FilterOptionUi? {
        val name = firstStringValue(
            "Name",
            "name",
            "CategoryName",
            "categoryName",
            "ManufacturerName",
            "manufacturerName",
            "VendorName",
            "vendorName",
            "Text",
            "text",
            "Value",
            "value",
            "Label",
            "label"
        ) ?: firstAvailableStringValue()

        val cleanName = name?.trim().orEmpty()
        if (cleanName.isEmpty()) return null

        val id = firstStringValue(
            "Id",
            "id",
            "CategoryId",
            "categoryId",
            "ManufacturerId",
            "manufacturerId",
            "VendorId",
            "vendorId",
            "Value",
            "value"
        ) ?: "$index-$cleanName"

        return FilterOptionUi(id = id, name = cleanName)
    }

    private fun JsonObject.firstStringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
        }
    }

    private fun JsonObject.firstAvailableStringValue(): String? {
        return entrySet().firstNotNullOfOrNull { (_, value) ->
            value.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
        }
    }

    private companion object {
        const val PRODUCT_PAGE_SIZE = 20
        const val PRODUCT_CACHE_PRODUCT_LIMIT = PRODUCT_PAGE_SIZE
        const val ACTIVE_STATUS = 1
        const val IN_STOCK_CODE = "S"
        const val CISCO_LABEL = "CISCO"
        const val COUNT_ERROR = "-"
        const val COMMON_MANUFACTURER_KEY = "ManufacturerName"
        const val COMMON_VENDOR_KEY = "vendor"
        const val COMMON_ATTRIBUTE_TAGS_KEY = "AttributeTags"
    }
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {
    private val _productState = MutableStateFlow<UiState<ProductDetailResponse>>(UiState.Idle)
    val productState = _productState.asStateFlow()

    fun loadProduct(id: String) {
        viewModelScope.launch {
            _productState.value = UiState.Loading
            _productState.value = productsRepository.getProductDetail(id).toUiState()
        }
    }
}

data class ProductListUiModel(
    val total: Int,
    val products: List<ProductListItemUi>,
    val isLoadingNextPage: Boolean = false,
    val isAppendUpdate: Boolean = false
)

data class ProductTabCountsUiModel(
    val all: String,
    val active: String,
    val inactive: String
) {
    companion object {
        fun empty() = ProductTabCountsUiModel(
            all = "-",
            active = "-",
            inactive = "-"
        )
    }
}

data class FilterOptionUi(
    val id: String,
    val name: String
)

data class ProductListItemUi(
    val id: String,
    val name: String,
    val sku: String,
    val manufacturer: String,
    val manufacturerPartNumber: String,
    val vendor: String,
    val category: String,
    val description: String,
    val countryOfOrigin: String,
    val screenSize: String,
    val dimensions: String,
    val unitOfMeasure: String,
    val weight: String,
    val warrantyPeriod: String,
    val validityPeriod: String,
    val vendors: List<ProductVendorUi>,
    val listPrice: String,
    val displayPrice: String,
    val lastUpdate: String,
    val availability: ProductAvailability,
    val status: ProductStatusFilter,
    val thumbnailLabel: String,
    val brandThumbnail: Boolean
)

data class ProductVendorUi(
    val name: String,
    val sku: String,
    val availability: String,
    val unit: String,
    val costPrice: String,
    val listPrice: String,
    val status: String
)

enum class ProductAvailability(val label: String) {
    InStock("In Stock"),
    OutOfStock("Out of Stock")
}

enum class ProductStatusFilter(val apiStatus: Int?) {
    All(null),
    Active(1),
    Inactive(0)
}
