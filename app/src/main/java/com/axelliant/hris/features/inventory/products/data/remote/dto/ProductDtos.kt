package com.axelliant.hris.features.inventory.products.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

object ProductSearchRequest {
    fun create(
        size: Int,
        from: Int,
        search: String? = null,
        status: Int? = null,
        filters: ProductSearchFilters = ProductSearchFilters(),
        includeNameInSearchPayload: Boolean = true
    ): JsonElement {
        val trimmedSearch = search?.trim().orEmpty()
        val page = (from / size.coerceAtLeast(1)) + 1

        return JsonObject().apply {
            addProperty("page", page)
            addProperty("size", size)
            if (trimmedSearch.isNotBlank()) {
                addProperty("search", trimmedSearch)
                addProperty("Name", if (includeNameInSearchPayload) trimmedSearch else "")
            }
            status?.let { addProperty("status", it) }
            addStringOrArray("Category", filters.categoryNames)
            addStringOrArray("ManufacturerName", filters.manufacturerNames)
            addStringOrArray("vendor", filters.vendorNames)
            addStringArray("AttributeTags", filters.attributeTags)
            addListPrice(filters.minListPrice, filters.maxListPrice)
            filters.availabilityCode?.let { addProperty("StockStatus", it) }
            filters.specificationOptions
                .filterValues { it.isNotEmpty() }
                .forEach { (key, values) -> addStringOrArray(key, values) }
        }
    }
}

data class ProductSearchFilters(
    val vendorNames: List<String> = emptyList(),
    val manufacturerNames: List<String> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val attributeTags: List<String> = emptyList(),
    val specificationOptions: Map<String, List<String>> = emptyMap(),
    val minListPrice: Double? = null,
    val maxListPrice: Double? = null,
    val availabilityCode: String? = null,
    val excludedAvailabilityCode: String? = null
)

private fun JsonObject.addStringOrArray(key: String, values: List<String>) {
    val cleanedValues = values.filter { it.isNotBlank() }.distinct()
    when (cleanedValues.size) {
        0 -> Unit
        1 -> addProperty(key, cleanedValues.first())
        else -> addStringArray(key, cleanedValues)
    }
}

private fun JsonObject.addStringArray(key: String, values: List<String>) {
    val cleanedValues = values.filter { it.isNotBlank() }.distinct()
    if (cleanedValues.isEmpty()) return
    add(key, com.google.gson.JsonArray().apply { cleanedValues.forEach(::add) })
}

private fun JsonObject.addListPrice(min: Double?, max: Double?) {
    if (min == null && max == null) return
    add(
        "ListPrice",
        JsonObject().apply {
            min?.let { addProperty("min", it) }
            max?.let { addProperty("max", it) }
        }
    )
}

data class ProductListResponse(
    @SerializedName("total")
    val total: Int? = null,
    @SerializedName("page")
    val page: Int? = null,
    @SerializedName("size")
    val size: Int? = null,
    @SerializedName("count")
    val count: Int? = null,
    @SerializedName("pages")
    val pages: Int? = null,
    @SerializedName("has_next")
    val hasNext: Boolean? = null,
    @SerializedName("has_prev")
    val hasPrevious: Boolean? = null,
    @SerializedName("active_count")
    val activeCount: Int? = null,
    @SerializedName("inactive_count")
    val inactiveCount: Int? = null,
    @SerializedName("products")
    val products: List<ProductSourceResponse> = emptyList(),
    @SerializedName("hits")
    val hits: ProductHitsResponse? = null
)

data class ProductHitsResponse(
    @SerializedName("total")
    val total: ProductTotalResponse? = null,
    @SerializedName("hits")
    val hits: List<ProductHitResponse> = emptyList()
)

data class ProductTotalResponse(
    @SerializedName("value")
    val value: Int = 0
)

data class ProductHitResponse(
    @SerializedName("_source")
    val source: ProductSourceResponse? = null
)

data class ProductSourceResponse(
    @SerializedName("Id")
    val id: String? = null,
    @SerializedName("Name")
    val name: String? = null,
    @SerializedName("Description")
    val description: String? = null,
    @SerializedName("MFGPartNo")
    val manufacturerPartNumber: String? = null,
    @SerializedName("AXEPartNo")
    val axePartNumber: String? = null,
    @SerializedName("UPC")
    val upc: String? = null,
    @SerializedName("UOM")
    val unitOfMeasure: String? = null,
    @SerializedName("ListPrice")
    val listPrice: Double? = null,
    @SerializedName("Status")
    val status: Int? = null,
    @SerializedName("ManufacturerName")
    val manufacturerName: String? = null,
    @SerializedName("StockStatus")
    val stockStatus: String? = null,
    @SerializedName("ScreenSize")
    val screenSize: String? = null,
    @SerializedName("Width")
    val width: Double? = null,
    @SerializedName("Height")
    val height: Double? = null,
    @SerializedName("Length")
    val length: Double? = null,
    @SerializedName("Weight")
    val weight: Double? = null,
    @SerializedName("WarrentyPeriod")
    val warrantyPeriod: String? = null,
    @SerializedName("ValidityPeriod")
    val validityPeriod: String? = null,
    @SerializedName("CountryOfOrigin")
    val countryOfOrigin: String? = null,
    @SerializedName("CountryOfOrignName")
    val countryOfOriginName: String? = null,
    @SerializedName("VendorInfo")
    val vendorInfo: List<ProductVendorResponse>? = null,
    @SerializedName("Images")
    val images: List<ProductImageResponse>? = null,
    @SerializedName("Category")
    val category: JsonElement? = null,
    @SerializedName("UpdatedOn")
    val updatedOn: Long? = null
)

data class ProductVendorResponse(
    @SerializedName("VendorPartNo")
    val vendorPartNumber: String? = null,
    @SerializedName("VendorSKU")
    val vendorSku: String? = null,
    @SerializedName("ListPrice")
    val listPrice: Double? = null,
    @SerializedName("Cost")
    val cost: Double? = null,
    @SerializedName("Qty")
    val quantity: Int? = null,
    @SerializedName("IsActive")
    val isActive: Boolean? = null,
    @SerializedName("ActiveFromVendor")
    val activeFromVendor: Boolean? = null,
    @SerializedName("VendorName")
    val vendorName: String? = null
)

data class ProductImageResponse(
    @SerializedName("PreviewUrl")
    val previewUrl: String? = null,
    @SerializedName("Url")
    val url: String? = null
)

data class ProductDetailResponse(
    val id: Int,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val sku: String? = null,
    val manufacturerName: String? = null,
    val manufacturerPartNumber: String? = null,
    val upc: String? = null,
    val countryOfOrigin: String? = null,
    val price: Double? = null,
    val discountPercentage: Double? = null,
    val rating: Double? = null,
    val stock: Int? = null,
    val brand: String? = null,
    val availabilityStatus: String? = null,
    val thumbnail: String? = null,
    val images: List<String> = emptyList(),
    val technicalSpecifications: Map<String, String> = emptyMap(),
    val vendors: List<VendorResponse> = emptyList(),
    val compliance: List<String> = emptyList()
)

data class VendorResponse(
    val id: String,
    val name: String,
    val sku: String? = null
)
