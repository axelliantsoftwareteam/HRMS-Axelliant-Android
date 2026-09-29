package com.axelliant.hris.features.purchaseorders.presentation

import androidx.annotation.IdRes
import com.axelliant.hris.R
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderStatus

object PurchaseOrderActionMenuResolver {

    @IdRes
    fun visibleActionIds(status: PurchaseOrderStatus): Set<Int> {
        return when (status) {
            PurchaseOrderStatus.DRAFT -> draftActions
            PurchaseOrderStatus.RELEASED -> openActions
            else -> unknownActions
        }
    }

    private val draftActions = setOf(
        R.id.action_purchase_order_view,
        R.id.action_purchase_order_edit,
        R.id.action_purchase_order_show_report,
        R.id.action_purchase_order_submit_po
    )

    private val openActions = setOf(
        R.id.action_purchase_order_view,
        R.id.action_purchase_order_show_report,
        R.id.action_purchase_order_send_to_vendor,
        R.id.action_purchase_order_history
    )

    private val unknownActions = setOf(
        R.id.action_purchase_order_view,
        R.id.action_purchase_order_show_report,
        R.id.action_purchase_order_history
    )
}
