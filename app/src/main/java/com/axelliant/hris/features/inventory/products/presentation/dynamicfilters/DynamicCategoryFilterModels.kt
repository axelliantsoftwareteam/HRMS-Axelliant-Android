package com.axelliant.hris.features.inventory.products.presentation.dynamicfilters

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class DynamicCategoryFilterPayload(
    @SerializedName("common_filters")
    val commonFilters: List<DynamicSpecFilter> = emptyList(),
    @SerializedName("categories")
    val categories: List<DynamicCategoryFilter> = emptyList()
)

data class DynamicCategoryFilter(
    @SerializedName("Name")
    val name: String = "",
    @SerializedName("is_selected")
    val isSelected: Boolean = false,
    @SerializedName("filters")
    val filters: List<DynamicSpecFilter> = emptyList()
)

data class DynamicSpecFilter(
    @SerializedName("key")
    val key: String = "",
    @SerializedName("label")
    val label: String = "",
    @SerializedName("type")
    val type: String = "",
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("is_selected")
    val isSelected: Boolean = false,
    @SerializedName("options")
    val options: List<DynamicSpecOption> = emptyList()
) {
    val selectionKey: String
        get() = key.ifBlank { label }

    val displayLabel: String
        get() = label.ifBlank { key }

    val allowsMultipleSelections: Boolean
        get() = type.equals("multiselect", ignoreCase = true)
}

data class DynamicSpecOption(
    @SerializedName("Name")
    val name: String = "",
    @SerializedName("is_selected")
    val isSelected: Boolean = false
)

data class SelectedCategorySpecs(
    val categoryName: String? = null,
    val selectedOptions: Map<String, List<String>> = emptyMap()
) {
    val hasCategory: Boolean
        get() = !categoryName.isNullOrBlank()

    val selectedOptionCount: Int
        get() = selectedOptions.values.sumOf { it.size }

    val hasSelectedOptions: Boolean
        get() = selectedOptionCount > 0
}

object DynamicCategorySpecsResult {
    const val REQUEST_KEY = "dynamic_category_specs_result"
    const val ARG_SELECTED_SPECS_JSON = "selected_specs_json"
    const val ARG_CATEGORY_NAME = "category_name"

    private val gson = Gson()

    fun toJson(specs: SelectedCategorySpecs): String = gson.toJson(specs)

    fun fromJson(json: String?): SelectedCategorySpecs {
        return runCatching {
            if (json.isNullOrBlank()) {
                SelectedCategorySpecs()
            } else {
                gson.fromJson(json, SelectedCategorySpecs::class.java) ?: SelectedCategorySpecs()
            }
        }.getOrDefault(SelectedCategorySpecs())
    }
}
