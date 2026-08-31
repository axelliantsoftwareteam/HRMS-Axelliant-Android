package com.axelliant.hris.features.quotes.domain.model

enum class QuoteWorkflowDisplayMode {
    Completed,
    InProgress
}

enum class QuoteWorkflowStepState {
    Approved,
    Active,
    Pending,
    Rejected,
    Skipped
}
data class QuoteWorkflowUiModel(
    val quoteNumber: String,
    val displayMode: QuoteWorkflowDisplayMode,
    val steps: List<QuoteWorkflowStepUiModel>
)

data class QuoteWorkflowStepUiModel(
    val id: String = "",
    val nodeType: String = "",
    val processNo: Int,
    val roleName: String,
    val processName: String,
    val processScreenName: String,
    val timestampDate: String,
    val approveText: String,
    val rejectText: String,
    val statusLabel: String,
    val stepState: QuoteWorkflowStepState,
    val comments: String = ""
) {
    val hasTimestamp: Boolean
        get() = timestampDate.isNotBlank() && timestampDate != "N/A"

    val hasComments: Boolean
        get() = comments.isNotBlank()
}
