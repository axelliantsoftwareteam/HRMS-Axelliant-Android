package com.axelliant.hris.features.quotes.presentation

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.axelliant.hris.R
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus

data class QuoteStatusUi(
    @StringRes val labelRes: Int,
    @DrawableRes val backgroundRes: Int,
    @ColorRes val textColorRes: Int
)

object QuoteStatusUiMapper {

    fun getStatusUi(approvalStatus: Int): QuoteStatusUi {
        return when (approvalStatus) {
            QuoteStatus.Rejected.apiValue -> mapInternal(QuoteStatus.Rejected)
            QuoteStatus.Submitted.apiValue -> mapInternal(QuoteStatus.Submitted)
            QuoteStatus.Approved.apiValue -> mapInternal(QuoteStatus.Approved)
            QuoteStatus.Draft.apiValue -> mapInternal(QuoteStatus.Draft)
            QuoteStatus.AwaitingApproval.apiValue -> mapInternal(QuoteStatus.AwaitingApproval)
            QuoteStatus.Cancelled.apiValue -> mapInternal(QuoteStatus.Cancelled)
            else -> unknownStatusUi()
        }
    }

    fun map(status: QuoteStatus): QuoteStatusUi = mapInternal(status)

    fun mapApprovalStatusLabel(status: String): QuoteStatusUi {
        val trimmed = status.trim()
        val apiValue = trimmed.toIntOrNull()
        return if (apiValue != null) {
            getStatusUi(apiValue)
        } else {
            when (trimmed.lowercase()) {
                "approved" -> mapInternal(QuoteStatus.Approved)
                "submitted" -> mapInternal(QuoteStatus.Submitted)
                "rejected" -> mapInternal(QuoteStatus.Rejected)
                "saveasdraft", "draft" -> mapInternal(QuoteStatus.Draft)
                "awaitingapproval", "awaiting approval" -> mapInternal(QuoteStatus.AwaitingApproval)
                "cancelled", "canceled" -> mapInternal(QuoteStatus.Cancelled)
                else -> unknownStatusUi()
            }
        }
    }

    private fun unknownStatusUi(): QuoteStatusUi {
        return QuoteStatusUi(
            labelRes = R.string.quote_status_unknown,
            backgroundRes = R.drawable.bg_quote_status_unknown,
            textColorRes = R.color.quotes_status_unknown_text
        )
    }

    private fun mapInternal(status: QuoteStatus): QuoteStatusUi = when (status) {
        QuoteStatus.Draft -> QuoteStatusUi(
            R.string.quote_status_draft,
            R.drawable.bg_quote_status_draft,
            R.color.quotes_status_draft_text
        )
        QuoteStatus.Approved -> QuoteStatusUi(
            R.string.quote_status_approved,
            R.drawable.bg_quote_status_approved,
            R.color.ds_success
        )
        QuoteStatus.Submitted -> QuoteStatusUi(
            R.string.quote_status_submitted,
            R.drawable.bg_quote_status_submitted,
            R.color.quotes_status_submitted_text
        )
        QuoteStatus.Rejected -> QuoteStatusUi(
            R.string.quote_status_rejected,
            R.drawable.bg_quote_status_rejected,
            R.color.quotes_status_rejected_text
        )
        QuoteStatus.AwaitingApproval -> QuoteStatusUi(
            R.string.quote_status_awaiting_approval,
            R.drawable.bg_quote_status_awaiting,
            R.color.quotes_status_awaiting_text
        )
        QuoteStatus.Cancelled -> QuoteStatusUi(
            R.string.quote_status_cancelled,
            R.drawable.bg_quote_status_cancelled,
            R.color.quotes_status_cancelled_text
        )
    }
}
