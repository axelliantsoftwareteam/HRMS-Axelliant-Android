package com.axelliant.hris.features.quotes.presentation

import com.axelliant.hris.R
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.axelliant.hris.features.quotes.domain.model.QuoteStatusFilterType
import com.axelliant.hris.features.quotes.domain.model.QuotesEmptyStateUi

object QuotesEmptyStateResolver {

    fun resolve(
        activeSearchQuery: String,
        selectedFilterType: QuoteStatusFilterType
    ): QuotesEmptyStateUi {
        if (activeSearchQuery.isNotBlank()) {
            return QuotesEmptyStateUi(
                titleRes = R.string.quotes_empty_search_title,
                descriptionRes = R.string.quotes_empty_search_description
            )
        }

        return when (selectedFilterType.quoteStatus) {
            QuoteStatus.Approved -> QuotesEmptyStateUi(
                R.string.quotes_empty_approved_title,
                R.string.quotes_empty_approved_description
            )
            QuoteStatus.Submitted -> QuotesEmptyStateUi(
                R.string.quotes_empty_submitted_title,
                R.string.quotes_empty_submitted_description
            )
            QuoteStatus.Rejected -> QuotesEmptyStateUi(
                R.string.quotes_empty_rejected_title,
                R.string.quotes_empty_rejected_description
            )
            QuoteStatus.Draft -> QuotesEmptyStateUi(
                R.string.quotes_empty_draft_title,
                R.string.quotes_empty_draft_description
            )
            else -> QuotesEmptyStateUi(
                R.string.quotes_empty_all_title,
                R.string.quotes_empty_all_description
            )
        }
    }
}
