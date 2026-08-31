package com.axelliant.hris.features.quotes.presentation

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemQuoteWorkflowStepBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowDisplayMode
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepState
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepUiModel

class QuoteWorkflowStepAdapter(
    private val displayMode: QuoteWorkflowDisplayMode,
    private val onApproveClick: (QuoteWorkflowStepUiModel) -> Unit = {},
    private val onRejectClick: (QuoteWorkflowStepUiModel) -> Unit = {}
) : ListAdapter<QuoteWorkflowStepUiModel, QuoteWorkflowStepAdapter.StepViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemQuoteWorkflowStepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(
            step = getItem(position),
            previousStepState = if (position > 0) getItem(position - 1).stepState else null,
            isFirst = position == 0,
            isLast = position == itemCount - 1
        )
    }

    inner class StepViewHolder(
        private val binding: ItemQuoteWorkflowStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            step: QuoteWorkflowStepUiModel,
            previousStepState: QuoteWorkflowStepState?,
            isFirst: Boolean,
            isLast: Boolean
        ) {
            val context = binding.root.context
            val mutedColor = ContextCompat.getColor(context, R.color.ds_text_muted)
            val primaryColor = ContextCompat.getColor(context, R.color.ds_text_primary)
            val secondaryColor = ContextCompat.getColor(context, R.color.ds_text_secondary)

            bindTimeline(step, previousStepState, isFirst, isLast)

            binding.processNumberText.text = step.processNo.toString()
            binding.roleNameText.text = buildRoleNameText(step.roleName)
            binding.processNameText.text = step.processName
            binding.processScreenNameText.text = step.processScreenName
            binding.roleNameText.isVisible = step.roleName != "N/A"
            binding.processScreenNameText.isVisible = step.processScreenName.isNotBlank()

            binding.approveButton.text = step.approveText
            binding.rejectButton.text = step.rejectText

            binding.approveButton.setOnClickListener(null)
            binding.rejectButton.setOnClickListener(null)

            binding.stepStatusIcon.clearColorFilter()

            binding.timestampText.isVisible = step.hasTimestamp

            if (step.hasTimestamp) {
                binding.timestampText.text = step.timestampDate
            }

            when (step.stepState) {
                QuoteWorkflowStepState.Approved -> {
                    bindApprovedStep(step)
                }

                QuoteWorkflowStepState.Active -> {
                    bindActiveStep(
                        step = step,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }

                QuoteWorkflowStepState.Pending -> {
                    bindPendingStep(mutedColor)
                }

                QuoteWorkflowStepState.Rejected -> {
                    bindRejectedStep(step)
                }

                QuoteWorkflowStepState.Skipped -> {
                    bindSkippedStep(mutedColor)
                }
            }
        }

        private fun bindTimeline(
            step: QuoteWorkflowStepUiModel,
            previousStepState: QuoteWorkflowStepState?,
            isFirst: Boolean,
            isLast: Boolean
        ) {
            val context = binding.root.context

            val outlineColor = ContextCompat.getColor(
                context,
                R.color.ds_outline
            )

            val successColor = ContextCompat.getColor(
                context,
                R.color.ds_success
            )

            val errorColor = ContextCompat.getColor(
                context,
                R.color.ds_error
            )

            binding.timelineTopLine.isVisible = !isFirst
            binding.timelineBottomLine.isVisible = !isLast

            binding.timelineTopLine.setBackgroundColor(
                when {
                    // Current step is rejected
                    step.stepState == QuoteWorkflowStepState.Rejected ->
                        errorColor

                    // Previous step was approved
                    previousStepState == QuoteWorkflowStepState.Approved ->
                        successColor

                    // Previous step was rejected
                    previousStepState == QuoteWorkflowStepState.Rejected ->
                        errorColor

                    else ->
                        outlineColor
                }
            )

            binding.timelineBottomLine.setBackgroundColor(
                when (step.stepState) {
                    QuoteWorkflowStepState.Approved ->
                        successColor

                    QuoteWorkflowStepState.Rejected ->
                        errorColor

                    else ->
                        outlineColor
                }
            )
        }
        private fun bindApprovedStep(step: QuoteWorkflowStepUiModel) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_completed)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_done)
            binding.stepStatusIcon.isVisible = true
            binding.stepStatusIcon.setImageResource(R.drawable.ia_ic_filter_check)
            binding.stepStatusIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, R.color.ia_white)
            )
            binding.processNumberText.isVisible = false
            binding.statusBadgeText.isVisible = true
            binding.waitingForActionText.isVisible = false
            binding.rejectionReasonText.isVisible = step.hasComments
            binding.rejectionReasonText.text = step.comments
            binding.actionButtonsRow.isVisible = false
            binding.statusBadgeText.text = step.statusLabel
            binding.statusBadgeText.setBackgroundResource(R.drawable.bg_quote_workflow_status_approved)
            binding.statusBadgeText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.quotes_status_approved_text)
            )
            setTextColors(
                titleColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_primary),
                bodyColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_secondary),
                mutedColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_muted),
                alpha = 1f
            )
        }
        private fun bindCompletedStep(step: QuoteWorkflowStepUiModel) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_completed)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_done)
            binding.stepStatusIcon.isVisible = true
            binding.stepStatusIcon.setImageResource(R.drawable.ia_ic_filter_check)
            binding.stepStatusIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, R.color.ia_white)
            )
            binding.processNumberText.isVisible = false
            binding.statusBadgeText.isVisible = true
            binding.waitingForActionText.isVisible = false
            binding.rejectionReasonText.isVisible = step.hasComments
            binding.rejectionReasonText.text = step.comments
            binding.actionButtonsRow.isVisible = false
            binding.statusBadgeText.text = step.statusLabel
            binding.statusBadgeText.setBackgroundResource(R.drawable.bg_quote_workflow_status_approved)
            binding.statusBadgeText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.quotes_status_approved_text)
            )
            setTextColors(
                titleColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_primary),
                bodyColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_secondary),
                mutedColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_muted),
                alpha = 1f
            )
        }


        private fun bindActiveStep(
            step: QuoteWorkflowStepUiModel,
            primaryColor: Int,
            secondaryColor: Int
        ) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_active)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_active)
            binding.stepStatusIcon.isVisible = false
            binding.processNumberText.isVisible = true
            binding.processNumberText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.ia_white)
            )
            binding.statusBadgeText.isVisible = false
            binding.waitingForActionText.isVisible = true
            binding.rejectionReasonText.isVisible = false
            binding.actionButtonsRow.isVisible = displayMode == QuoteWorkflowDisplayMode.InProgress
            binding.approveButton.setBackgroundResource(R.drawable.bg_quote_workflow_btn_approve)
            binding.rejectButton.setBackgroundResource(R.drawable.bg_quote_workflow_btn_reject)
            binding.approveButton.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.ia_white)
            )
            binding.rejectButton.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.ds_error)
            )
            binding.approveButton.isEnabled = true
            binding.rejectButton.isEnabled = true
            binding.approveButton.alpha = 1f
            binding.rejectButton.alpha = 1f
            binding.approveButton.setOnClickListener { onApproveClick(step) }
            binding.rejectButton.setOnClickListener { onRejectClick(step) }
            setTextColors(primaryColor, secondaryColor, secondaryColor, 1f)
        }

        private fun bindSkippedStep(mutedColor: Int) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_pending)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_pending)
            binding.stepStatusIcon.isVisible = false
            binding.processNumberText.isVisible = true
            binding.processNumberText.setTextColor(mutedColor)
            binding.statusBadgeText.isVisible = true
            binding.waitingForActionText.isVisible = false
            binding.rejectionReasonText.isVisible = false
            binding.actionButtonsRow.isVisible = false
            binding.statusBadgeText.text = binding.root.context.getString(
                R.string.quote_workflow_status_skipped
            )
            binding.statusBadgeText.setBackgroundResource(R.drawable.bg_quote_workflow_status_pending)
            binding.statusBadgeText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.quotes_status_unknown_text)
            )
            binding.approveButton.isEnabled = false
            binding.rejectButton.isEnabled = false
            setTextColors(mutedColor, mutedColor, mutedColor, 0.85f)
        }
        private fun bindPendingStep(mutedColor: Int) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_pending)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_pending)
            binding.stepStatusIcon.isVisible = false
            binding.processNumberText.isVisible = true
            binding.processNumberText.setTextColor(mutedColor)
            binding.statusBadgeText.isVisible = true
            binding.waitingForActionText.isVisible = false
            binding.rejectionReasonText.isVisible = false
            binding.actionButtonsRow.isVisible = false
            binding.statusBadgeText.text = binding.root.context.getString(
                R.string.quote_workflow_status_pending
            )
            binding.statusBadgeText.setBackgroundResource(R.drawable.bg_quote_workflow_status_pending)
            binding.statusBadgeText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.quotes_status_unknown_text)
            )
            binding.approveButton.isEnabled = false
            binding.rejectButton.isEnabled = false
            setTextColors(mutedColor, mutedColor, mutedColor, 0.85f)
        }

        private fun bindRejectedStep(step: QuoteWorkflowStepUiModel) {
            binding.stepCard.setBackgroundResource(R.drawable.bg_quote_workflow_step_rejected)
            binding.nodeContainer.setBackgroundResource(R.drawable.bg_quote_workflow_node_rejected)
            binding.stepStatusIcon.isVisible = true
            binding.processNumberText.isVisible = false
            binding.stepStatusIcon.setImageResource(R.drawable.ic_quote_action_cancel)
            binding.stepStatusIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, R.color.ia_white)
            )
            binding.statusBadgeText.isVisible = true
            binding.waitingForActionText.isVisible = false
            binding.rejectionReasonText.isVisible = true
            binding.actionButtonsRow.isVisible = false
            binding.statusBadgeText.text = binding.root.context.getString(
                R.string.quote_workflow_status_rejected
            )
            binding.statusBadgeText.setBackgroundResource(R.drawable.bg_quote_workflow_status_rejected)
            binding.statusBadgeText.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.quotes_status_rejected_text)
            )
            binding.rejectionReasonText.text = step.comments.ifBlank {
                binding.root.context.getString(
                    R.string.quote_workflow_rejected_by_format,
                    step.roleName
                )
            }
            setTextColors(
                titleColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_primary),
                bodyColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_secondary),
                mutedColor = ContextCompat.getColor(binding.root.context, R.color.ds_text_muted),
                alpha = 1f
            )
        }

        private fun setTextColors(
            titleColor: Int,
            bodyColor: Int,
            mutedColor: Int,
            alpha: Float
        ) {
            binding.processNameText.setTextColor(titleColor)
            binding.roleNameText.setTextColor(bodyColor)
            binding.processScreenNameText.setTextColor(mutedColor)
            binding.timestampText.setTextColor(bodyColor)
            binding.roleNameText.alpha = alpha
            binding.processNameText.alpha = alpha
            binding.processScreenNameText.alpha = alpha
            binding.timestampText.alpha = alpha
        }

        private fun buildRoleNameText(roleName: String): SpannableString {
            val label = binding.root.context.getString(R.string.quote_workflow_role_name_label)
            return SpannableString(label + roleName).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuoteWorkflowStepUiModel>() {
        override fun areItemsTheSame(
            oldItem: QuoteWorkflowStepUiModel,
            newItem: QuoteWorkflowStepUiModel
        ): Boolean = oldItem.processNo == newItem.processNo

        override fun areContentsTheSame(
            oldItem: QuoteWorkflowStepUiModel,
            newItem: QuoteWorkflowStepUiModel
        ): Boolean = oldItem == newItem
    }
}
