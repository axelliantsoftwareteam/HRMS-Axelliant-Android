package com.axelliant.hris.features.quotes.presentation

import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.axelliant.hris.R
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import androidx.core.view.size
import androidx.core.view.get

class QuoteActionMenuHandler(
    private val onActionSelected: (actionId: Int, quote: QuoteModel) -> Unit
) {
    fun show(anchor: View, quote: QuoteModel) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_quote_actions, popupMenu.menu)

        val visibleActions = QuoteActionMenuResolver.visibleActionIds(quote.status)
        val menu = popupMenu.menu
        for (index in 0 until menu.size) {
            val item = menu[index]
            item.isVisible = item.itemId in visibleActions
        }

        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId in visibleActions) {
                onActionSelected(item.itemId, quote)
                true
            } else {
                false
            }
        }
        popupMenu.show()
    }
}
