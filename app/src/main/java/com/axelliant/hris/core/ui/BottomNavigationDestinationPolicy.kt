package com.axelliant.hris.core.ui

import androidx.annotation.IdRes
import com.axelliant.hris.R

/**
 * Central policy for which navigation destinations should show the main bottom bar.
 * Update [hiddenDestinationIds] when adding screens that require a full-height content area.
 */
object BottomNavigationDestinationPolicy {

    private val hiddenDestinationIds: Set<Int> = setOf(
        R.id.commonLoginFragment,
        R.id.iaSettingsFragment,
        R.id.iaViewQuoteFragment,
        R.id.iaQuoteReportFragment,
        R.id.iaAddQuoteFragment,
        R.id.iaAddQuoteAddressFragment,
        R.id.iaSmartQuoteFragment,
        R.id.iaAddQuoteProductFragment,
        R.id.iaAskAiProductSearchFragment,
        R.id.iaProductCodeScannerFragment,
        R.id.iaDynamicCategorySelectionFragment,
        R.id.iaDynamicCategorySpecsFragment,
        R.id.iaProductDetailFragment,
        R.id.iaProductComparisonFragment,
        R.id.iaSaleOrdersFragment,
        R.id.iaWarehousesFragment,
        R.id.iaAddSaleOrderFragment,
        R.id.iaViewSaleOrderFragment,
        R.id.iaPurchaseOrdersFragment,
        R.id.iaAddWarehouseFragment,
        R.id.iaWarehouseLocationsFragment,
        R.id.iaAddWarehouseLocationFragment,
        R.id.iaWarehouseInventoryFragment,
        R.id.iaWarehouseReceivingFragment,
        R.id.iaAddPurchaseOrderFragment,
        R.id.iaCreateManualPurchaseOrderFragment,
        R.id.iaViewPurchaseOrderFragment,
        R.id.iaEditPurchaseOrderFragment,
    )

    fun shouldShowBottomNavigation(@IdRes destinationId: Int): Boolean {
        return destinationId !in hiddenDestinationIds
    }
}
