package com.axelliant.hris.features.saleorders.presentation

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.core.view.size
import com.axelliant.hris.R
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderModel

class SaleOrderActionMenuHandler(
    private val onActionSelected: (actionId: Int, order: SaleOrderModel) -> Unit
) {
    fun show(anchor: View, order: SaleOrderModel) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_sale_order_actions, popupMenu.menu)

        val visibleActions = SaleOrderActionMenuResolver.visibleActionIds(order.status)
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
