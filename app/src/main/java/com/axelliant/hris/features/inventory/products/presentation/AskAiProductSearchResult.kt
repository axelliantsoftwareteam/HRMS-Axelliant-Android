package com.axelliant.hris.features.inventory.products.presentation

import android.os.Bundle
import com.axelliant.hris.features.inventory.products.ai.ProductAiAvailability
import com.axelliant.hris.features.inventory.products.ai.ProductAiCategorySpecSuggestion
import com.axelliant.hris.features.inventory.products.ai.ProductAiFilterOption
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.DynamicCategorySpecsResult
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.SelectedCategorySpecs

object AskAiProductSearchResult {
    const val REQUEST_KEY = "ask_ai_product_search_result"

    private const val SEARCH_TEXT = "search_text"
    private const val STATUS = "status"
    private const val AVAILABILITY = "availability"
    private const val MIN_PRICE = "min_price"
    private const val MAX_PRICE = "max_price"
    private const val CATEGORY_IDS = "category_ids"
    private const val CATEGORY_NAMES = "category_names"
    private const val MANUFACTURER_IDS = "manufacturer_ids"
    private const val MANUFACTURER_NAMES = "manufacturer_names"
    private const val VENDOR_IDS = "vendor_ids"
    private const val VENDOR_NAMES = "vendor_names"
    private const val DYNAMIC_CATEGORY_SPECS = "dynamic_category_specs"

    fun toBundle(
        searchText: String,
        status: ProductAiStatus?,
        availability: ProductAiAvailability?,
        minPrice: Double?,
        maxPrice: Double?,
        categories: List<ProductAiFilterOption>,
        manufacturers: List<ProductAiFilterOption>,
        vendors: List<ProductAiFilterOption>,
        dynamicCategorySpecs: ProductAiCategorySpecSuggestion = ProductAiCategorySpecSuggestion()
    ): Bundle {
        return Bundle().apply {
            putString(SEARCH_TEXT, searchText)
            status?.let { putString(STATUS, it.name) }
            availability?.let { putString(AVAILABILITY, it.name) }
            minPrice?.let { putDouble(MIN_PRICE, it) }
            maxPrice?.let { putDouble(MAX_PRICE, it) }
            putStringArrayList(CATEGORY_IDS, ArrayList(categories.map { it.id }))
            putStringArrayList(CATEGORY_NAMES, ArrayList(categories.map { it.name }))
            putStringArrayList(MANUFACTURER_IDS, ArrayList(manufacturers.map { it.id }))
            putStringArrayList(MANUFACTURER_NAMES, ArrayList(manufacturers.map { it.name }))
            putStringArrayList(VENDOR_IDS, ArrayList(vendors.map { it.id }))
            putStringArrayList(VENDOR_NAMES, ArrayList(vendors.map { it.name }))
            putString(
                DYNAMIC_CATEGORY_SPECS,
                DynamicCategorySpecsResult.toJson(
                    SelectedCategorySpecs(
                        categoryName = dynamicCategorySpecs.categoryName,
                        selectedOptions = dynamicCategorySpecs.selectedOptions
                    )
                )
            )
        }
    }

    fun searchText(bundle: Bundle): String = bundle.getString(SEARCH_TEXT).orEmpty()

    fun status(bundle: Bundle): ProductAiStatus? {
        return bundle.getString(STATUS)?.let { runCatching { ProductAiStatus.valueOf(it) }.getOrNull() }
    }

    fun availability(bundle: Bundle): ProductAiAvailability? {
        return bundle.getString(AVAILABILITY)?.let {
            runCatching { ProductAiAvailability.valueOf(it) }.getOrNull()
        }
    }

    fun minPrice(bundle: Bundle): Double? = bundle.getNullableDouble(MIN_PRICE)

    fun maxPrice(bundle: Bundle): Double? = bundle.getNullableDouble(MAX_PRICE)

    fun categories(bundle: Bundle): List<ProductAiFilterOption> = options(bundle, CATEGORY_IDS, CATEGORY_NAMES)

    fun manufacturers(bundle: Bundle): List<ProductAiFilterOption> = options(bundle, MANUFACTURER_IDS, MANUFACTURER_NAMES)

    fun vendors(bundle: Bundle): List<ProductAiFilterOption> = options(bundle, VENDOR_IDS, VENDOR_NAMES)

    fun dynamicCategorySpecs(bundle: Bundle): SelectedCategorySpecs {
        return DynamicCategorySpecsResult.fromJson(bundle.getString(DYNAMIC_CATEGORY_SPECS))
    }

    private fun options(bundle: Bundle, idsKey: String, namesKey: String): List<ProductAiFilterOption> {
        val ids = bundle.getStringArrayList(idsKey).orEmpty()
        val names = bundle.getStringArrayList(namesKey).orEmpty()
        return ids.mapIndexedNotNull { index, id ->
            val name = names.getOrNull(index).orEmpty()
            if (id.isBlank() || name.isBlank()) null else ProductAiFilterOption(id = id, name = name)
        }
    }

    private fun Bundle.getNullableDouble(key: String): Double? {
        return if (containsKey(key)) getDouble(key) else null
    }

}
