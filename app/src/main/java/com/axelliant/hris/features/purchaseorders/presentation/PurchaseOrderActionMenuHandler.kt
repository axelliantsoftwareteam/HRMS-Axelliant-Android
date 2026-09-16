package com.axelliant.hris.features.purchaseorders.presentation

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.core.view.size
import com.axelliant.hris.R
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel

class PurchaseOrderActionMenuHandler(
    private val onActionSelected: (actionId: Int, order: PurchaseOrderModel) -> Unit
) {
    fun show(anchor: View, order: PurchaseOrderModel) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_purchase_order_actions, popupMenu.menu)

        val visibleActions = PurchaseOrderActionMenuResolver.visibleActionIds(order.status)
        val menu = popupMenu.menu
        for (index in 0 until menu.size) {
            val item = menu[index]
            item.isVisible = item.itemId in visibleActions
        }

        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId in visibleActions) {
                onActionSelected(item.itemId, order)
                true
            } else {
                false
            }
        }
        popupMenu.show()
    }
}
