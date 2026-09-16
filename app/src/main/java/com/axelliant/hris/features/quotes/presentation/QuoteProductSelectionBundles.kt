package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
import androidx.core.os.bundleOf
import com.axelliant.hris.features.inventory.products.presentation.ProductAvailability
import com.axelliant.hris.features.inventory.products.presentation.ProductListItemUi
import com.axelliant.hris.features.inventory.products.presentation.ProductStatusFilter
import com.axelliant.hris.features.quotes.domain.model.QuoteCreationProductUi
import java.text.NumberFormat
import java.util.Locale

object QuoteProductSelectionBundles {
    fun fromProducts(products: List<QuoteCreationProductUi>): Bundle {
        return bundleOf(
            AddQuoteProductFragment.KEY_PRODUCT_IDS to ArrayList(products.map { it.id }),
            AddQuoteProductFragment.KEY_PRODUCT_NAMES to ArrayList(products.map { it.name }),
            AddQuoteProductFragment.KEY_PRODUCT_SKUS to ArrayList(products.map { it.sku }),
            AddQuoteProductFragment.KEY_PRODUCT_CATEGORIES to ArrayList(products.map { it.category }),
            AddQuoteProductFragment.KEY_PRODUCT_THUMBNAILS to ArrayList(products.map { it.thumbnailLabel }),
            AddQuoteProductFragment.KEY_PRODUCT_BRAND_THUMBNAILS to BooleanArray(products.size) { index ->
                products[index].brandThumbnail
            },
            AddQuoteProductFragment.KEY_PRODUCT_PRICES to DoubleArray(products.size) { index ->
                products[index].unitPrice
            }
        )
    }

    fun Bundle.toQuoteCreationProducts(): List<QuoteCreationProductUi> {
        val ids = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_IDS).orEmpty()
        val names = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_NAMES).orEmpty()
        val skus = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_SKUS).orEmpty()
        val categories = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_CATEGORIES).orEmpty()
        val thumbnails = getStringArrayList(AddQuoteProductFragment.KEY_PRODUCT_THUMBNAILS).orEmpty()
        val brandThumbnails = getBooleanArray(AddQuoteProductFragment.KEY_PRODUCT_BRAND_THUMBNAILS)
        val prices = getDoubleArray(AddQuoteProductFragment.KEY_PRODUCT_PRICES)
        return ids.mapIndexed { index, id ->
            QuoteCreationProductUi(
                id = id,
                name = names.getOrNull(index).orEmpty(),
                sku = skus.getOrNull(index).orEmpty(),
                category = categories.getOrNull(index).orEmpty(),
                thumbnailLabel = thumbnails.getOrNull(index).orEmpty(),
                brandThumbnail = brandThumbnails?.getOrNull(index) ?: false,
                unitPrice = prices?.getOrNull(index) ?: 0.0
            )
        }
    }

    fun Bundle.toPreselectedProductItems(): LinkedHashMap<String, ProductListItemUi> {
        return toQuoteCreationProducts().associate { product ->
            product.id to product.toProductListItemUi()
        }.let { LinkedHashMap(it) }
    }
}

fun ProductListItemUi.toQuoteCreationProduct(): QuoteCreationProductUi {
    return QuoteCreationProductUi(
        id = id,
        name = name,
        sku = sku,
        category = category,
        thumbnailLabel = thumbnailLabel,
        brandThumbnail = brandThumbnail,
        unitPrice = displayPrice.toCurrencyDouble()
    )
}

private fun QuoteCreationProductUi.toProductListItemUi(): ProductListItemUi {
    val formattedPrice = NumberFormat.getCurrencyInstance(Locale.US).format(unitPrice)
    return ProductListItemUi(
        id = id,
        name = name,
        sku = sku,
        manufacturer = "",
        manufacturerPartNumber = "",
        vendor = "",
        category = category,
        description = "",
        countryOfOrigin = "",
        screenSize = "",
        dimensions = "",
        unitOfMeasure = "",
        weight = "",
        warrantyPeriod = "",
        validityPeriod = "",
        vendors = emptyList(),
        listPrice = formattedPrice,
        displayPrice = formattedPrice,
        lastUpdate = "",
        availability = ProductAvailability.InStock,
        status = ProductStatusFilter.Active,
        thumbnailLabel = thumbnailLabel,
        brandThumbnail = brandThumbnail
    )
}

private fun String.toCurrencyDouble(): Double {
    return replace("$", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull() ?: 0.0
}
