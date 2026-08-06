package com.axelliant.hris.features.inventory.products.presentation

import android.os.Bundle
import androidx.core.os.bundleOf

object ProductPickerResultBundles {
    private const val KEY_ID = "productId"
    private const val KEY_NAME = "productName"
    private const val KEY_SKU = "productSku"
    private const val KEY_MANUFACTURER = "productManufacturer"
    private const val KEY_MANUFACTURER_PART_NUMBER = "productManufacturerPartNumber"
    private const val KEY_VENDOR = "productVendor"
    private const val KEY_CATEGORY = "productCategory"
    private const val KEY_DESCRIPTION = "productDescription"
    private const val KEY_COUNTRY_OF_ORIGIN = "productCountryOfOrigin"
    private const val KEY_SCREEN_SIZE = "productScreenSize"
    private const val KEY_DIMENSIONS = "productDimensions"
    private const val KEY_UNIT_OF_MEASURE = "productUnitOfMeasure"
    private const val KEY_WEIGHT = "productWeight"
    private const val KEY_WARRANTY_PERIOD = "productWarrantyPeriod"
    private const val KEY_VALIDITY_PERIOD = "productValidityPeriod"
    private const val KEY_VENDOR_NAMES = "productVendorNames"
    private const val KEY_VENDOR_SKUS = "productVendorSkus"
    private const val KEY_VENDOR_AVAILABILITIES = "productVendorAvailabilities"
    private const val KEY_VENDOR_UNITS = "productVendorUnits"
    private const val KEY_VENDOR_COST_PRICES = "productVendorCostPrices"
    private const val KEY_VENDOR_LIST_PRICES = "productVendorListPrices"
    private const val KEY_VENDOR_STATUSES = "productVendorStatuses"
    private const val KEY_LIST_PRICE = "productListPrice"
    private const val KEY_DISPLAY_PRICE = "productDisplayPrice"
    private const val KEY_LAST_UPDATE = "productLastUpdate"
    private const val KEY_AVAILABILITY = "productAvailability"
    private const val KEY_STATUS = "productStatus"
    private const val KEY_THUMBNAIL_LABEL = "productThumbnailLabel"
    private const val KEY_BRAND_THUMBNAIL = "productBrandThumbnail"

    fun fromProduct(product: ProductListItemUi): Bundle {
        return bundleOf(
            KEY_ID to product.id,
            KEY_NAME to product.name,
            KEY_SKU to product.sku,
            KEY_MANUFACTURER to product.manufacturer,
            KEY_MANUFACTURER_PART_NUMBER to product.manufacturerPartNumber,
            KEY_VENDOR to product.vendor,
            KEY_CATEGORY to product.category,
            KEY_DESCRIPTION to product.description,
            KEY_COUNTRY_OF_ORIGIN to product.countryOfOrigin,
            KEY_SCREEN_SIZE to product.screenSize,
            KEY_DIMENSIONS to product.dimensions,
            KEY_UNIT_OF_MEASURE to product.unitOfMeasure,
            KEY_WEIGHT to product.weight,
            KEY_WARRANTY_PERIOD to product.warrantyPeriod,
            KEY_VALIDITY_PERIOD to product.validityPeriod,
            KEY_VENDOR_NAMES to ArrayList(product.vendors.map { it.name }),
            KEY_VENDOR_SKUS to ArrayList(product.vendors.map { it.sku }),
            KEY_VENDOR_AVAILABILITIES to ArrayList(product.vendors.map { it.availability }),
            KEY_VENDOR_UNITS to ArrayList(product.vendors.map { it.unit }),
            KEY_VENDOR_COST_PRICES to ArrayList(product.vendors.map { it.costPrice }),
            KEY_VENDOR_LIST_PRICES to ArrayList(product.vendors.map { it.listPrice }),
            KEY_VENDOR_STATUSES to ArrayList(product.vendors.map { it.status }),
            KEY_LIST_PRICE to product.listPrice,
            KEY_DISPLAY_PRICE to product.displayPrice,
            KEY_LAST_UPDATE to product.lastUpdate,
            KEY_AVAILABILITY to product.availability.name,
            KEY_STATUS to product.status.name,
            KEY_THUMBNAIL_LABEL to product.thumbnailLabel,
            KEY_BRAND_THUMBNAIL to product.brandThumbnail
        )
    }

    fun Bundle.toProductListItem(): ProductListItemUi? {
        val id = getString(KEY_ID).orEmpty()
        if (id.isBlank()) return null
        val vendorNames = getStringArrayList(KEY_VENDOR_NAMES).orEmpty()
        val vendorSkus = getStringArrayList(KEY_VENDOR_SKUS).orEmpty()
        val vendorAvailabilities = getStringArrayList(KEY_VENDOR_AVAILABILITIES).orEmpty()
        val vendorUnits = getStringArrayList(KEY_VENDOR_UNITS).orEmpty()
        val vendorCostPrices = getStringArrayList(KEY_VENDOR_COST_PRICES).orEmpty()
        val vendorListPrices = getStringArrayList(KEY_VENDOR_LIST_PRICES).orEmpty()
        val vendorStatuses = getStringArrayList(KEY_VENDOR_STATUSES).orEmpty()
        val vendors = vendorNames.mapIndexed { index, name ->
            ProductVendorUi(
                name = name,
                sku = vendorSkus.getOrNull(index).orEmpty(),
                availability = vendorAvailabilities.getOrNull(index).orEmpty(),
                unit = vendorUnits.getOrNull(index).orEmpty(),
                costPrice = vendorCostPrices.getOrNull(index).orEmpty(),
                listPrice = vendorListPrices.getOrNull(index).orEmpty(),
                status = vendorStatuses.getOrNull(index).orEmpty()
            )
        }
        return ProductListItemUi(
            id = id,
            name = getString(KEY_NAME).orEmpty(),
            sku = getString(KEY_SKU).orEmpty(),
            manufacturer = getString(KEY_MANUFACTURER).orEmpty(),
            manufacturerPartNumber = getString(KEY_MANUFACTURER_PART_NUMBER).orEmpty(),
            vendor = getString(KEY_VENDOR).orEmpty(),
            category = getString(KEY_CATEGORY).orEmpty(),
            description = getString(KEY_DESCRIPTION).orEmpty(),
            countryOfOrigin = getString(KEY_COUNTRY_OF_ORIGIN).orEmpty(),
            screenSize = getString(KEY_SCREEN_SIZE).orEmpty(),
            dimensions = getString(KEY_DIMENSIONS).orEmpty(),
            unitOfMeasure = getString(KEY_UNIT_OF_MEASURE).orEmpty(),
            weight = getString(KEY_WEIGHT).orEmpty(),
            warrantyPeriod = getString(KEY_WARRANTY_PERIOD).orEmpty(),
            validityPeriod = getString(KEY_VALIDITY_PERIOD).orEmpty(),
            vendors = vendors,
            listPrice = getString(KEY_LIST_PRICE).orEmpty(),
            displayPrice = getString(KEY_DISPLAY_PRICE).orEmpty(),
            lastUpdate = getString(KEY_LAST_UPDATE).orEmpty(),
            availability = enumValueOrDefault(
                getString(KEY_AVAILABILITY),
                ProductAvailability.InStock
            ),
            status = enumValueOrDefault(
                getString(KEY_STATUS),
                ProductStatusFilter.Active
            ),
            thumbnailLabel = getString(KEY_THUMBNAIL_LABEL).orEmpty(),
            brandThumbnail = getBoolean(KEY_BRAND_THUMBNAIL)
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T {
        return runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)
    }
}
