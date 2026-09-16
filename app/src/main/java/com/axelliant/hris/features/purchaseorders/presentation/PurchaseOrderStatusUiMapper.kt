package com.axelliant.hris.features.purchaseorders.presentation

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.axelliant.hris.R
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderStatus
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilization

data class PurchaseOrderBadgeUi(
    @StringRes val labelRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val textColorRes: Int
)

object PurchaseOrderStatusUiMapper {

    fun mapStatus(status: PurchaseOrderStatus): PurchaseOrderBadgeUi = when (status) {
        PurchaseOrderStatus.DRAFT -> PurchaseOrderBadgeUi(
            R.string.purchase_order_status_draft,
            R.drawable.bg_sale_order_status_draft,
            R.color.sale_order_status_draft_text
        )
        PurchaseOrderStatus.RELEASED -> PurchaseOrderBadgeUi(
            R.string.purchase_order_status_released,
            R.drawable.bg_purchase_order_status_open,
            R.color.purchase_order_green_text
        )
        PurchaseOrderStatus.PENDING -> PurchaseOrderBadgeUi(
            R.string.purchase_order_status_pending,
            R.drawable.bg_sale_order_status_pending,
            R.color.sale_order_status_pending_text
        )
        PurchaseOrderStatus.CANCELED -> PurchaseOrderBadgeUi(
            R.string.purchase_order_status_canceled,
            R.drawable.bg_sale_order_status_canceled,
            R.color.sale_order_status_canceled_text
        )
        PurchaseOrderStatus.APPROVED -> PurchaseOrderBadgeUi(
            R.string.purchase_order_status_approved,
            R.drawable.bg_sale_order_status_approved,
            R.color.sale_order_status_approved_text
        )
    }

    fun mapUtilization(utilization: PurchaseOrderUtilization): PurchaseOrderBadgeUi = when (utilization) {
        PurchaseOrderUtilization.PARTIALLY_UTILIZED -> PurchaseOrderBadgeUi(
            R.string.purchase_order_util_partially,
            R.drawable.bg_purchase_order_util_partial,
            R.color.purchase_order_orange_text
        )
        PurchaseOrderUtilization.FULLY_UTILIZED -> PurchaseOrderBadgeUi(
            R.string.purchase_order_util_fully,
            R.drawable.bg_purchase_order_util_fully_soft,
            R.color.purchase_order_green_text
        )
        PurchaseOrderUtilization.PENDING -> PurchaseOrderBadgeUi(
            R.string.purchase_order_util_pending,
            R.drawable.bg_sale_order_util_pending,
            R.color.purchase_order_util_full_text
        )
    }
}
