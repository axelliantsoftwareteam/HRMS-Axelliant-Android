package com.axelliant.hris.features.inventory.products.ai

data class ProductAiFilterOption(
    val id: String,
    val name: String
)

data class ProductAiContextFilter(
    val key: String,
    val label: String,
    val type: String,
    val options: List<ProductAiFilterOption> = emptyList()
) {
    val selectionKey: String
        get() = key.ifBlank { label }

    val allowsMultipleSelections: Boolean
        get() = type.equals("multiselect", ignoreCase = true)
}

data class ProductAiCategoryContext(
    val name: String,
    val filters: List<ProductAiContextFilter> = emptyList()
)

data class ProductAiCategorySpecSuggestion(
    val categoryName: String? = null,
    val selectedOptions: Map<String, List<String>> = emptyMap()
) {
    val hasCategory: Boolean
        get() = !categoryName.isNullOrBlank()

    val hasSelectedOptions: Boolean
        get() = selectedOptions.values.any { it.isNotEmpty() }
}

data class ProductAiSearchContext(
    val vendors: List<ProductAiFilterOption> = emptyList(),
    val manufacturers: List<ProductAiFilterOption> = emptyList(),
    val categories: List<ProductAiFilterOption> = emptyList(),
    val commonFilters: List<ProductAiContextFilter> = emptyList(),
    val dynamicCategories: List<ProductAiCategoryContext> = emptyList()
) {
    fun optionsForCommonKey(key: String): List<ProductAiFilterOption> {
        return commonFilters
            .firstOrNull { filter -> filter.selectionKey.equals(key, ignoreCase = true) }
            ?.options
            .orEmpty()
    }

    val hasJsonFilters: Boolean
        get() = commonFilters.isNotEmpty() || dynamicCategories.isNotEmpty()
}

data class ParsedProductAiFilter(
    val originalQuery: String,
    val queryIntent: ProductAiQueryIntent = ProductAiQueryIntent.UNCLEAR,
    val searchText: String = "",
    val manufacturers: List<ProductAiFilterOption> = emptyList(),
    val vendors: List<ProductAiFilterOption> = emptyList(),
    val categories: List<ProductAiFilterOption> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val exactPrice: Double? = null,
    val dynamicCategorySpecs: ProductAiCategorySpecSuggestion = ProductAiCategorySpecSuggestion(),
    val availability: ProductAiAvailability? = null,
    val status: ProductAiStatus? = null,
    val confidenceScore: Int = 0,
    val reasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val confidenceLevel: ProductAiConfidenceLevel
        get() = when {
            confidenceScore >= 75 -> ProductAiConfidenceLevel.High
            confidenceScore >= 45 -> ProductAiConfidenceLevel.Medium
            else -> ProductAiConfidenceLevel.Low
        }

    val hasMeaningfulFilters: Boolean
        get() = searchText.isNotBlank() ||
            manufacturers.isNotEmpty() ||
            vendors.isNotEmpty() ||
            categories.isNotEmpty() ||
            minPrice != null ||
            maxPrice != null ||
            exactPrice != null ||
            dynamicCategorySpecs.hasCategory ||
            availability != null ||
            status != null
}

enum class ProductAiAvailability(
    val displayName: String,
    val stockCode: String?,
    val excludedStockCode: String? = null
) {
    InStock("In Stock", "S"),
    OutOfStock("Out of Stock", null, "S")
}

enum class ProductAiStatus(
    val displayName: String,
    val apiStatus: Int?
) {
    All("All", null),
    Active("Active", 1),
    Inactive("Inactive", 0)
}

enum class ProductAiConfidenceLevel {
    High,
    Medium,
    Low
}

enum class ProductAiQueryIntent {
    PRODUCT_SEARCH,
    FILTER_ONLY,
    MIXED_SEARCH,
    UNCLEAR
}

enum class ProductAiFilterType {
    SearchText,
    Manufacturer,
    Vendor,
    Category,
    MinPrice,
    MaxPrice,
    Availability,
    Status
}
