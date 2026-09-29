package com.axelliant.hris.features.saleorders.presentation

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.axelliant.hris.R
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatus
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderUtilization

data class SaleOrderBadgeUi(
    @StringRes val labelRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val textColorRes: Int
)

object SaleOrderStatusUiMapper {

    fun mapStatus(status: SaleOrderStatus): SaleOrderBadgeUi = when (status) {
        SaleOrderStatus.DRAFT -> SaleOrderBadgeUi(
            R.string.sale_order_status_draft,
            R.drawable.bg_sale_order_status_draft,
            R.color.sale_order_status_draft_text
        )
        SaleOrderStatus.RELEASED -> SaleOrderBadgeUi(
            R.string.sale_order_status_released,
            R.drawable.bg_sale_order_status_released,
            R.color.sale_order_status_released_text
        )
        SaleOrderStatus.PENDING -> SaleOrderBadgeUi(
            R.string.sale_order_status_pending,
            R.drawable.bg_sale_order_status_pending,
            R.color.sale_order_status_pending_text
        )
        SaleOrderStatus.CANCELED -> SaleOrderBadgeUi(
            R.string.sale_order_status_canceled,
            R.drawable.bg_sale_order_status_canceled,
            R.color.sale_order_status_canceled_text
        )
        SaleOrderStatus.APPROVED -> SaleOrderBadgeUi(
            R.string.sale_order_status_approved,
            R.drawable.bg_sale_order_status_approved,
            R.color.sale_order_status_approved_text
        )
    }

    fun mapUtilization(utilization: SaleOrderUtilization): SaleOrderBadgeUi = when (utilization) {
        SaleOrderUtilization.PARTIALLY_UTILIZED -> SaleOrderBadgeUi(
            R.string.sale_order_util_partially,
            R.drawable.bg_sale_order_util_partial,
            R.color.sale_order_util_partial_text
        )
        SaleOrderUtilization.FULLY_UTILIZED -> SaleOrderBadgeUi(
            R.string.sale_order_util_fully,
            R.drawable.bg_sale_order_util_full,
            R.color.sale_order_util_full_text
        )
        SaleOrderUtilization.PENDING -> SaleOrderBadgeUi(
            R.string.sale_order_util_pending,
            R.drawable.bg_sale_order_util_pending,
            R.color.sale_order_util_partial_text
        )
    }
}
