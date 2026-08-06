package com.axelliant.hris.features.inventory.products.presentation

import com.axelliant.hris.features.inventory.products.ai.ProductAiAvailability
import com.axelliant.hris.features.inventory.products.ai.ProductAiStatus
import com.axelliant.hris.features.inventory.products.presentation.dynamicfilters.SelectedCategorySpecs

data class ProductFilterSheetState(
    var searchText: String = "",
    var status: ProductAiStatus? = null,
    val categories: LinkedHashMap<String, String> = linkedMapOf(),
    val manufacturers: LinkedHashMap<String, String> = linkedMapOf(),
    val vendors: LinkedHashMap<String, String> = linkedMapOf(),
    val attributeTags: LinkedHashMap<String, String> = linkedMapOf(),
    var dynamicCategorySpecs: SelectedCategorySpecs = SelectedCategorySpecs(),
    var categoryOptions: List<FilterOptionUi> = emptyList(),
    var manufacturerOptions: List<FilterOptionUi> = emptyList(),
    var vendorOptions: List<FilterOptionUi> = emptyList(),
    var attributeTagOptions: List<FilterOptionUi> = emptyList(),
    var minListPrice: Double? = null,
    var maxListPrice: Double? = null,
    var availability: ProductAiAvailability? = null
) {
    fun copyForEditing(): ProductFilterSheetState {
        return ProductFilterSheetState(
            searchText = searchText,
            status = status,
            categories = LinkedHashMap(categories),
            manufacturers = LinkedHashMap(manufacturers),
            vendors = LinkedHashMap(vendors),
            attributeTags = LinkedHashMap(attributeTags),
            dynamicCategorySpecs = dynamicCategorySpecs.copy(
                selectedOptions = dynamicCategorySpecs.selectedOptions.mapValues { (_, values) -> values.toList() }
            ),
            categoryOptions = categoryOptions,
            manufacturerOptions = manufacturerOptions,
            vendorOptions = vendorOptions,
            attributeTagOptions = attributeTagOptions,
            minListPrice = minListPrice,
            maxListPrice = maxListPrice,
            availability = availability
        )
    }
}
