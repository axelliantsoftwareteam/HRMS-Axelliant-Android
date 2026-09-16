package com.axelliant.hris.features.inventory.products.presentation.dynamicfilters

import android.content.Context
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.R
import com.axelliant.hris.features.inventory.products.data.ProductsRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticCategoryFilterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productsRepository: ProductsRepository
) {
    private var cachedPayload: DynamicCategoryFilterPayload? = null

    suspend fun prefetch() {
        getPayload()
    }

    suspend fun getCategories(): List<DynamicCategoryFilter> {
        return getPayload().categories.filter { it.name.isNotBlank() }
    }

    suspend fun getCommonFilters(): List<DynamicSpecFilter> {
        return getPayload().commonFilters.filter { it.selectionKey.isNotBlank() }
    }

    private suspend fun getPayload(): DynamicCategoryFilterPayload {
        cachedPayload?.let { return it }

        val apiPayload = when (val result = productsRepository.getAdvancedProductFilters()) {
            is ApiResult.Success -> result.data.toDynamicCategoryFilterPayload()
            else -> null
        }
        if (apiPayload != null && (apiPayload.commonFilters.isNotEmpty() || apiPayload.categories.isNotEmpty())) {
            cachedPayload = apiPayload
            return apiPayload
        }

        return getStaticPayload().also { cachedPayload = it }
    }

    private fun getStaticPayload(): DynamicCategoryFilterPayload {
        val json = context.resources.openRawResource(R.raw.dynamic_category_filters)
            .bufferedReader()
            .use { it.readText().trimStart('\uFEFF', '\u200B') }
        return Gson()
            .fromJson(json, DynamicCategoryFilterPayload::class.java)
            ?: DynamicCategoryFilterPayload()
    }

    private fun JsonElement.toDynamicCategoryFilterPayload(): DynamicCategoryFilterPayload? {
        val staticPayload = runCatching {
            Gson().fromJson(this, DynamicCategoryFilterPayload::class.java)
        }.getOrNull()
        if (staticPayload != null && (staticPayload.commonFilters.isNotEmpty() || staticPayload.categories.isNotEmpty())) {
            return staticPayload
        }

        val filters = extractFilterArray()
            .mapNotNull { element -> element.asJsonObjectOrNull()?.toDynamicSpecFilter() }
            .filter { it.selectionKey.isNotBlank() }
        if (filters.isEmpty()) return null

        val categoryFilter = filters.firstOrNull { it.selectionKey.equals(CATEGORY_KEY, ignoreCase = true) }
        val categoryOptions = categoryFilter
            ?.options
            .orEmpty()
            .mapNotNull { option -> option.name.trim().takeIf { it.isNotEmpty() } }
            .distinctBy { it.lowercase() }

        val categorizedFilters = filters
            .filterNot { it.selectionKey.equals(CATEGORY_KEY, ignoreCase = true) }
            .filter { !it.category.isNullOrBlank() }
            .groupBy { it.category.orEmpty() }

        val categories = when {
            categoryOptions.isNotEmpty() -> categoryOptions.map { categoryName ->
                DynamicCategoryFilter(
                    name = categoryName,
                    filters = categorizedFilters[categoryName].orEmpty()
                )
            }
            categorizedFilters.isNotEmpty() -> categorizedFilters.map { (categoryName, specs) ->
                DynamicCategoryFilter(name = categoryName, filters = specs)
            }
            else -> emptyList()
        }

        val commonFilters = filters.filterNot { filter ->
            filter.selectionKey.equals(CATEGORY_KEY, ignoreCase = true) || !filter.category.isNullOrBlank()
        }
        return DynamicCategoryFilterPayload(
            commonFilters = commonFilters,
            categories = categories
        )
    }

    private fun JsonElement.extractFilterArray(): List<JsonElement> {
        if (isJsonArray) return asJsonArray.toList()
        val jsonObject = asJsonObjectOrNull() ?: return emptyList()
        FILTER_ARRAY_KEYS.forEach { key ->
            jsonObject.get(key)?.extractFilterArray()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return jsonObject.entrySet()
            .asSequence()
            .mapNotNull { (_, value) -> value.extractFilterArray().takeIf { it.isNotEmpty() } }
            .firstOrNull()
            .orEmpty()
    }

    private fun JsonObject.toDynamicSpecFilter(): DynamicSpecFilter? {
        val key = firstStringValue("key", "Key", "field", "Field", "name", "Name").orEmpty().trim()
        val label = firstStringValue("label", "Label", "displayName", "DisplayName", "title", "Title")
            .orEmpty()
            .trim()
            .ifBlank { key }
        if (key.isBlank() && label.isBlank()) return null

        return DynamicSpecFilter(
            key = key.ifBlank { label },
            label = label,
            type = firstStringValue("type", "Type", "filterType", "FilterType").orEmpty().trim(),
            category = firstStringValue("category", "Category", "categoryName", "CategoryName")?.trim(),
            options = get("options")?.toDynamicSpecOptions()
                ?: get("Options")?.toDynamicSpecOptions()
                ?: get("values")?.toDynamicSpecOptions()
                ?: get("Values")?.toDynamicSpecOptions()
                ?: emptyList()
        )
    }

    private fun JsonElement.toDynamicSpecOptions(): List<DynamicSpecOption> {
        val elements = when {
            isJsonArray -> asJsonArray.toList()
            isJsonObject -> asJsonObject.entrySet().map { it.value }
            else -> emptyList()
        }
        return elements.mapNotNull { element ->
            when {
                element.isJsonPrimitive -> element.asString.trim().takeIf { it.isNotEmpty() }
                element.isJsonObject -> element.asJsonObject.firstStringValue(
                    "Name",
                    "name",
                    "label",
                    "Label",
                    "value",
                    "Value",
                    "text",
                    "Text"
                )?.trim()
                else -> null
            }?.let { DynamicSpecOption(name = it) }
        }.distinctBy { it.name.lowercase() }
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return if (isJsonObject) asJsonObject else null
    }

    private fun JsonObject.firstStringValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { primitive ->
                primitive.isString || primitive.isNumber || primitive.isBoolean
            }?.asString
        }
    }

    private companion object {
        const val CATEGORY_KEY = "Category"
        val FILTER_ARRAY_KEYS = listOf(
            "filters",
            "Filters",
            "data",
            "Data",
            "result",
            "Result",
            "items",
            "Items"
        )
    }
}
