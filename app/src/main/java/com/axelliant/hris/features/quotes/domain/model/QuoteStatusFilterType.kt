package com.axelliant.hris.features.quotes.domain.model

import androidx.annotation.StringRes
import com.axelliant.hris.R

enum class QuoteStatusFilterType(
    @StringRes val labelRes: Int
) {
    ALL(R.string.quotes_filter_label_all),
    APPROVED(R.string.quotes_filter_label_approved),
    SUBMITTED(R.string.quotes_filter_label_submitted),
    REJECTED(R.string.quotes_filter_label_rejected),
    DRAFT(R.string.quotes_filter_label_draft);

    val filterId: String
        get() = name.lowercase()

    val apiApprovalStatus: Int?
        get() = when (this) {
            ALL -> null
            APPROVED -> QuoteStatus.Approved.apiValue
            SUBMITTED -> QuoteStatus.Submitted.apiValue
            REJECTED -> QuoteStatus.Rejected.apiValue
            DRAFT -> QuoteStatus.Draft.apiValue
        }

    val quoteStatus: QuoteStatus?
        get() = when (this) {
            ALL -> null
            APPROVED -> QuoteStatus.Approved
            SUBMITTED -> QuoteStatus.Submitted
            REJECTED -> QuoteStatus.Rejected
            DRAFT -> QuoteStatus.Draft
        }

    companion object {
        fun fromFilterId(filterId: String): QuoteStatusFilterType {
            return entries.firstOrNull { it.filterId == filterId } ?: ALL
        }
    }
}
