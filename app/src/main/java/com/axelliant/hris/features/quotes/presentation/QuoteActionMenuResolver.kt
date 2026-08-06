package com.axelliant.hris.features.quotes.presentation

import androidx.annotation.IdRes
import com.axelliant.hris.R
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus

object QuoteActionMenuResolver {

    @IdRes
    fun visibleActionIds(status: QuoteStatus?): Set<Int> {
        return when (status) {
            QuoteStatus.Draft -> draftActions
            QuoteStatus.Submitted -> submittedActions
            QuoteStatus.Approved -> approvedActions
            QuoteStatus.Cancelled -> cancelledActions
            QuoteStatus.AwaitingApproval -> awaitingApprovalActions
            else -> defaultActions
        }
    }

    private val draftActions = setOf(
        R.id.action_quote_view,
        R.id.action_quote_edit,
        R.id.action_quote_duplicate,
        R.id.action_quote_show_report
    )

    private val submittedActions = setOf(
        R.id.action_quote_view,
        R.id.action_quote_revise,
        R.id.action_quote_duplicate,
        R.id.action_quote_submit_workflow,
        R.id.action_quote_show_report,
        R.id.action_quote_cancel
    )

    private val approvedActions = setOf(
        R.id.action_quote_view,
        R.id.action_quote_duplicate,
        R.id.action_quote_view_workflow,
        R.id.action_quote_show_report,
        R.id.action_quote_cancel
    )

    private val cancelledActions = setOf(
        R.id.action_quote_view,
        R.id.action_quote_duplicate,
        R.id.action_quote_submit_workflow,
        R.id.action_quote_show_report,
        R.id.action_quote_cancel
    )

    private val awaitingApprovalActions = setOf(
        R.id.action_quote_view,
        R.id.action_quote_revise,
        R.id.action_quote_duplicate,
        R.id.action_quote_submit_workflow,
        R.id.action_quote_audit_logs,
        R.id.action_quote_workflow_logs,
        R.id.action_quote_show_report,
        R.id.action_quote_cancel
    )

    private val defaultActions = setOf(R.id.action_quote_view)
}
