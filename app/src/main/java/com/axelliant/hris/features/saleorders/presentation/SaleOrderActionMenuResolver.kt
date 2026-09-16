package com.axelliant.hris.features.saleorders.presentation

import androidx.annotation.IdRes
import com.axelliant.hris.R
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatus

object SaleOrderActionMenuResolver {

    @IdRes
    fun visibleActionIds(status: SaleOrderStatus): Set<Int> {
        return when (status) {
            SaleOrderStatus.DRAFT -> draftActions
            SaleOrderStatus.RELEASED -> releasedActions
            else -> otherActions
        }
    }

    private val draftActions = setOf(
        R.id.action_sale_order_edit,
        R.id.action_sale_order_view,
        R.id.action_sale_order_show_report
    )

    private val releasedActions = setOf(
        R.id.action_sale_order_view,
        R.id.action_sale_order_show_report,
        R.id.action_sale_order_submit_workflow
    )

    private val otherActions = setOf(
        R.id.action_sale_order_view,
        R.id.action_sale_order_show_report,
        R.id.action_sale_order_submit_workflow
    )
}
