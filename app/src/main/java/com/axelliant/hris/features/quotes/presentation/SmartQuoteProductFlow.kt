package com.axelliant.hris.features.quotes.presentation

object SmartQuoteProductFlow {
    const val ARG_PRODUCT_PICKER_FLOW = "productPickerFlow"
    const val FLOW_SMART_QUOTE = "smart_quote"
    const val FLOW_ADD_QUOTE = "add_quote"
    const val FLOW_ADD_SALE_ORDER = "add_sale_order"
    const val FLOW_CREATE_MANUAL_PO = "create_manual_po"
    const val FLOW_EDIT_PO = "edit_po"
    const val FLOW_DEFAULT = "default"

    const val ARG_SELECTION_MODE = "selectionMode"
    const val ARG_ASK_AI_RESULT = "askAiResult"
    const val ARG_PRESELECTED_PRODUCTS = "preselectedProducts"
    const val ARG_INITIAL_SEARCH_QUERY = "initialSearchQuery"

    fun isLineItemPickerFlow(flow: String): Boolean {
        return flow == FLOW_SMART_QUOTE ||
            flow == FLOW_ADD_QUOTE ||
            flow == FLOW_ADD_SALE_ORDER ||
            flow == FLOW_CREATE_MANUAL_PO ||
            flow == FLOW_EDIT_PO
    }
}
