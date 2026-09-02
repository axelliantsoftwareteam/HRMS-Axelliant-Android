package com.axelliant.hris.features.quotes.presentation

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class QuoteWorkflowBottomSheet(
    private val fragment: Fragment,
    private val workflow: QuoteWorkflowUiModel,
    private val titleResId: Int = R.string.quote_workflow_title,
    private val onApproveClick: (QuoteWorkflowStepUiModel, String) -> Unit = { _, _ -> },
    private val onRejectClick: (QuoteWorkflowStepUiModel, String) -> Unit = { _, _ -> }
) {

    private var dialog: BottomSheetDialog? = null

    fun show() {
        val inflater = LayoutInflater.from(fragment.requireContext())
        val binding = BottomSheetQuoteWorkflowBinding.inflate(inflater)
        val sheetDialog = fragment.requireContext().createAppBottomSheetDialog()
        dialog = sheetDialog

        val stepAdapter = QuoteWorkflowStepAdapter(
            displayMode = workflow.displayMode,
            onApproveClick = { step ->
                showCommentDialog(
                    step = step,
                    title = fragment.getString(R.string.quote_workflow_approve_title),
                    actionText = fragment.getString(R.string.quote_workflow_approve_action),
                    isRejectAction = false,
                    onSubmit = onApproveClick
                )
            },
            onRejectClick = { step ->
                showCommentDialog(
                    step = step,
                    title = fragment.getString(R.string.quote_workflow_reject_title),
                    actionText = fragment.getString(R.string.quote_workflow_reject_action),
                    isRejectAction = true,
                    onSubmit = onRejectClick
                )
            }
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

    private fun showCommentDialog(
        step: QuoteWorkflowStepUiModel,
        title: String,
        actionText: String,
        isRejectAction: Boolean,
        onSubmit: (QuoteWorkflowStepUiModel, String) -> Unit
    ) {
        val context = fragment.requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                24.dp(context),
                4.dp(context),
                24.dp(context),
                8.dp(context)
            )
        }

        val description = TextView(context).apply {
            text = context.getString(R.string.quote_workflow_comments_optional)
            setTextAppearance(R.style.TextAppearance_Fluent2_Body)
            setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary))
            setPadding(0, 0, 0, 16.dp(context))
        }

        val inputLayout = TextInputLayout(
            context,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = context.getString(R.string.quote_workflow_comments_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(
                8.dp(context).toFloat(),
                8.dp(context).toFloat(),
                8.dp(context).toFloat(),
                8.dp(context).toFloat()
            )
            helperText = context.getString(R.string.quote_workflow_optional)
            counterMaxLength = 500
            isCounterEnabled = true
        }

        val commentInput = TextInputEditText(context).apply {
            minLines = 3
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false)
            setPadding(
                16.dp(context),
                14.dp(context),
                16.dp(context),
                14.dp(context)
            )
        }

        inputLayout.addView(
            commentInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(description)
        container.addView(
            inputLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val actionDialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(actionText, null)
            .create()

        actionDialog.setOnShowListener {
            val positiveButton = actionDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = actionDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.isAllCaps = false
            negativeButton.isAllCaps = false

            if (isRejectAction) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.ds_error))
            }

            positiveButton.setOnClickListener {
                onSubmit(step, commentInput.text?.toString().orEmpty().trim())
                actionDialog.dismiss()
            }
        }

        actionDialog.show()
        actionDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
        binding.workflowProgressIndicator.isVisible = false
        binding.workflowProgressText.text = context.getString(
            R.string.quote_workflow_completed_count_format,
            completedCount,
            totalSteps
        )
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

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}



