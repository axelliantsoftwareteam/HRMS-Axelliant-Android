package com.axelliant.hris.features.quotes.domain.model

import android.content.Context
data class QuoteFilterChip(
    val count: Int,
    val status: QuoteStatusFilterType
) {
    val filterId: String
        get() = status.filterId

    val apiApprovalStatus: Int?
        get() = status.apiApprovalStatus

    fun label(context: Context): String = context.getString(status.labelRes)
}
