package com.axelliant.hris.features.saleorders.domain.model

import android.content.Context
import androidx.annotation.StringRes
import com.axelliant.hris.R

enum class SaleOrderStatus {
    DRAFT,
    RELEASED,
    PENDING,
    CANCELED,
    APPROVED
}

enum class SaleOrderUtilization {
    PARTIALLY_UTILIZED,
    FULLY_UTILIZED,
    PENDING
}

enum class SaleOrderStatusFilterType(
    val filterId: String,
    @StringRes val labelRes: Int,
    val status: SaleOrderStatus?
) {
    ALL("all", R.string.sale_orders_filter_all, null),
    RELEASED("released", R.string.sale_orders_filter_released, SaleOrderStatus.RELEASED),
    PENDING("pending", R.string.sale_orders_filter_pending, SaleOrderStatus.PENDING),
    CANCELED("canceled", R.string.sale_orders_filter_canceled, SaleOrderStatus.CANCELED)
}

data class SaleOrderModel(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val status: SaleOrderStatus,
    val utilization: SaleOrderUtilization,
    val grandTotal: String,
    val deliveryDate: String
)

data class SaleOrderFilterChip(
    val count: Int,
    val filterType: SaleOrderStatusFilterType
) {
    val filterId: String
        get() = filterType.filterId

    fun label(context: Context): String = context.getString(filterType.labelRes)
}

data class SaleOrdersEmptyStateUi(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

data class SaleOrderListUiModel(
    val orders: List<SaleOrderModel>,
    val filterChips: List<SaleOrderFilterChip>,
    val totalCount: Int = 0,
    val isLoadingNextPage: Boolean = false,
    val isLastPage: Boolean = false,
    val emptyState: SaleOrdersEmptyStateUi? = null
)

data class SaleOrderPageResult(
    val orders: List<SaleOrderModel>,
    val totalCount: Int,
    val statusCounts: Map<SaleOrderStatusFilterType, Int>
)
