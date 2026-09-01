package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowGraphInstanceDto
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowDisplayMode
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepState
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel

object QuoteWorkflowGraphMapper {

    fun toUiModel(
        quoteNumber: String,
        workflow: WorkflowGraphInstanceDto
    ): QuoteWorkflowUiModel {

        val nodes = workflow.nodes
            .orEmpty()
            .sortedBy { it.sequence ?: Int.MAX_VALUE }

        val steps = nodes
            .mapIndexed { index, node ->

                QuoteWorkflowStepUiModel(
                    id = node.id.orEmpty(),

                    nodeType = node.nodeType.orEmpty(),

                    processNo = node.sequence ?: (index + 1),

                    roleName = node.roleIds
                        .orEmpty()
                        .joinToString(", ")
                        .ifBlank { getFallbackRoleName(node.nodeType) },

                    processName = node.processName.orEmpty(),

                    processScreenName = getProcessScreenName(node.nodeType),

                    timestampDate = node.completedOn
                        ?.let { formatTimestamp(it) }
                        ?: "N/A",

                    approveText = "Approved",

                    rejectText = "Rejected",

                    statusLabel = getStatusLabel(node.state),

                    stepState = getStepState(node.state),

                    comments = node.comments.orEmpty().trim()
                )
            }

        val displayMode =
            if (
                workflow.state.equals("approved", ignoreCase = true) ||
                workflow.state.equals("rejected", ignoreCase = true) ||
                nodes.none { it.state.equals("active", ignoreCase = true) }
            ) {
                QuoteWorkflowDisplayMode.Completed
            } else {
                QuoteWorkflowDisplayMode.InProgress
            }

        return QuoteWorkflowUiModel(
            quoteNumber = quoteNumber,
            displayMode = displayMode,
            steps = steps
        )
    }

    private fun getStepState(
        state: String?
    ): QuoteWorkflowStepState {
        return when (state?.lowercase()) {
            "approved" -> QuoteWorkflowStepState.Approved
            "active" -> QuoteWorkflowStepState.Active
            "pending" -> QuoteWorkflowStepState.Pending
            "rejected" -> QuoteWorkflowStepState.Rejected
            "skipped" -> QuoteWorkflowStepState.Skipped
            else -> QuoteWorkflowStepState.Pending
        }
    }

    private fun getStatusLabel(
        state: String?
    ): String {
        return when (state?.lowercase()) {
            "approved" -> "Approved"
            "active" -> "In Progress"
            "pending" -> "Pending"
            "rejected" -> "Rejected"
            "skipped" -> "Skipped"
            else -> "Pending"
        }
    }

    private fun getFallbackRoleName(nodeType: String?): String {
        return when (nodeType?.lowercase()) {
            "submission", "end" -> "System"
            else -> "N/A"
        }
    }

    private fun getProcessScreenName(nodeType: String?): String {
        return when (nodeType?.lowercase()) {
            "submission" -> "Submission"
            "end" -> "Workflow"
            "approval" -> "Approval"
            else -> "Workflow"
        }
    }

    private fun formatTimestamp(
        timestamp: Long
    ): String {
        // API timestamp is Unix seconds
        return java.text.SimpleDateFormat(
            "MMM dd, yyyy hh:mm a",
            java.util.Locale.getDefault()
        ).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date(timestamp * 1000))
    }
}
