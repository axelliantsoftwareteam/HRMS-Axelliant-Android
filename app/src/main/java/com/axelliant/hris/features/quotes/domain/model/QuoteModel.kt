package com.axelliant.hris.features.quotes.domain.model

enum class QuoteStatus(val apiValue: Int) {
    Rejected(0),
    Submitted(3),
    Approved(4),
    Draft(5),
    AwaitingApproval(6),
    Cancelled(7);

    companion object {
        fun fromApiValue(value: Int?): QuoteStatus? {
            if (value == null) return null
            return entries.firstOrNull { it.apiValue == value }
        }
    }
}

enum class QuoteType {
    Standard,
    Quick
}

data class QuoteModel(
    val id: String,
    val quoteId: String,
    val customerName: String,
    val approvalStatus: Int,
    val createdBy: String,
    val date: String,
    val totalAmount: String,
    val quoteType: QuoteType,
    val paymentTerm: String = "",
    val validityDays: String = "",
    val utilizingStatus: String = "",
    val notes: String = ""
) {
    val status: QuoteStatus?
        get() = QuoteStatus.fromApiValue(approvalStatus)
}

data class QuotesEmptyStateUi(
    val titleRes: Int,
    val descriptionRes: Int
)

data class QuoteListUiModel(
    val quotes: List<QuoteModel>,
    val filterChips: List<QuoteFilterChip>,
    val totalCount: Int = 0,
    val isLoadingNextPage: Boolean = false,
    val isLastPage: Boolean = false,
    val emptyState: QuotesEmptyStateUi? = null
)
