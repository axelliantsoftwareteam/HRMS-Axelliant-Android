package com.axelliant.hris.features.quotes.presentation

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.ui.designsystem.components.createAppBottomSheetDialog
import com.axelliant.hris.databinding.BottomSheetQuoteWorkflowBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepState
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class QuoteWorkflowBottomSheet(
    private val fragment: Fragment,
    private val workflow: QuoteWorkflowUiModel,
    private val titleResId: Int = R.string.quote_workflow_title
) {

    private var dialog: BottomSheetDialog? = null

    fun show() {
        val inflater = LayoutInflater.from(fragment.requireContext())
        val binding = BottomSheetQuoteWorkflowBinding.inflate(inflater)
        val sheetDialog = fragment.requireContext().createAppBottomSheetDialog()
        dialog = sheetDialog

        val stepAdapter = QuoteWorkflowStepAdapter(
            displayMode = workflow.displayMode,
            onApproveClick = { showComingSoon() },
            onRejectClick = { showComingSoon() }
        )

        binding.workflowTitleText.text = fragment.getString(titleResId)
        binding.quoteNumberText.text = workflow.quoteNumber
        bindWorkflowSummary(binding)
        binding.closeButton.setOnClickListener { sheetDialog.dismiss() }
        binding.workflowStepsRecyclerView.apply {
            layoutManager = LinearLayoutManager(fragment.requireContext())
            adapter = stepAdapter
            isNestedScrollingEnabled = true
            clipToPadding = false
        }
        stepAdapter.submitList(workflow.steps) {
            val activeIndex = workflow.steps.indexOfFirst {
                it.stepState == QuoteWorkflowStepState.Active
            }
            if (activeIndex >= 0) {
                binding.workflowStepsRecyclerView.smoothScrollToPosition(activeIndex)
            }
        }

        sheetDialog.setContentView(binding.root)
        sheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = ContextCompat.getColor(
                fragment.requireContext(),
                R.color.ds_surface
            )
        }
        sheetDialog.setOnShowListener {
            val bottomSheet = sheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            bottomSheet.background = ContextCompat.getDrawable(
                fragment.requireContext(),
                R.drawable.bg_filter_sheet
            )
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(bottom = systemBars.bottom)
                insets
            }
        }
        sheetDialog.show()
    }

    private fun showComingSoon() {
        Toast.makeText(fragment.requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
    }

    private fun bindWorkflowSummary(binding: BottomSheetQuoteWorkflowBinding) {
        val context = fragment.requireContext()
        val totalSteps = workflow.steps.size
        val completedCount = workflow.steps.count {
            it.stepState == QuoteWorkflowStepState.Approved ||
                it.stepState == QuoteWorkflowStepState.Rejected ||
                it.stepState == QuoteWorkflowStepState.Skipped
        }
        val allApproved = totalSteps > 0 && workflow.steps.all {
            it.stepState == QuoteWorkflowStepState.Approved
        }
        val rejected = workflow.steps.any { it.stepState == QuoteWorkflowStepState.Rejected }
        val activeStep = workflow.steps.firstOrNull {
            it.stepState == QuoteWorkflowStepState.Active
        }
        val progressColor = ContextCompat.getColor(
            context,
            when {
                rejected -> R.color.ds_error
                allApproved -> R.color.ds_success
                else -> R.color.ds_primary
            }
        )
        val currentStage = when {
            rejected -> context.getString(R.string.quote_workflow_status_rejected)
            allApproved -> context.getString(R.string.quote_workflow_status_approved)
            activeStep != null -> activeStep.processName
            else -> context.getString(R.string.quote_workflow_status_waiting)
        }
        val progress = if (totalSteps > 0) {
            ((completedCount.toFloat() / totalSteps.toFloat()) * 100).toInt()
        } else {
            0
        }

        binding.workflowProgressIndicator.progress = progress
        binding.workflowProgressIndicator.setIndicatorColor(progressColor)
        binding.workflowProgressText.text = "$completedCount/$totalSteps"
        binding.workflowCompletedText.text = context.getString(
            R.string.quote_workflow_completed_count_format,
            completedCount,
            totalSteps
        )
        binding.workflowCurrentText.text = buildCurrentText(currentStage, progressColor)
        binding.workflowCompleteNoticeContainer.isVisible = allApproved
    }

    private fun buildCurrentText(currentStage: String, currentColor: Int): SpannableString {
        val prefix = fragment.getString(R.string.quote_workflow_current_prefix)
        return SpannableString(prefix + currentStage).apply {
            setSpan(
                ForegroundColorSpan(currentColor),
                prefix.length,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}



