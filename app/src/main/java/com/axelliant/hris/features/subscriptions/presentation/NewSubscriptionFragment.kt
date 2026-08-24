package com.axelliant.hris.features.subscriptions.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentNewSubscriptionBinding
import com.axelliant.hris.extention.showSuccessMsg

class NewSubscriptionFragment : Fragment() {

    private var _binding: FragmentNewSubscriptionBinding? = null
    private val binding get() = _binding!!
    private var step = STEP_DETAILS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.appTopBar.setOnActionClickListener {
            requireContext().showSuccessMsg(getString(R.string.subscriptions_more_actions_coming_soon))
        }
        binding.backButton.setOnClickListener {
            if (step == STEP_DETAILS) {
                findNavController().navigateUp()
            } else {
                renderStep(step - 1)
            }
        }
        binding.primaryButton.setOnClickListener {
            if (step == STEP_REVIEW) {
                requireContext().showSuccessMsg(getString(R.string.subscription_create))
                findNavController().navigateUp()
            } else {
                renderStep(step + 1)
            }
        }
        renderStep(STEP_DETAILS)
    }

    private fun renderStep(nextStep: Int) {
        step = nextStep
        binding.detailsContent.isVisible = step == STEP_DETAILS
        binding.itemsContent.isVisible = step == STEP_ITEMS
        binding.reviewContent.isVisible = step == STEP_REVIEW

        binding.subtitleText.setText(
            when (step) {
                STEP_ITEMS -> R.string.new_subscription_items_subtitle
                STEP_REVIEW -> R.string.new_subscription_review_subtitle
                else -> R.string.new_subscription_details_subtitle
            }
        )
        binding.backButton.setText(if (step == STEP_DETAILS) R.string.subscription_cancel else R.string.subscription_back)
        binding.primaryButton.setText(
            when (step) {
                STEP_ITEMS -> R.string.subscription_continue_review
                STEP_REVIEW -> R.string.subscription_create
                else -> R.string.subscription_continue_items
            }
        )

        bindStepCircle(binding.detailsStepCircle, binding.detailsStepLabel, STEP_DETAILS)
        bindStepCircle(binding.itemsStepCircle, binding.itemsStepLabel, STEP_ITEMS)
        bindStepCircle(binding.reviewStepCircle, binding.reviewStepLabel, STEP_REVIEW)
        binding.contentScroll.scrollTo(0, 0)
    }

    private fun bindStepCircle(
        circle: com.axelliant.hris.ui.designsystem.components.AppTextView,
        label: com.axelliant.hris.ui.designsystem.components.AppTextView,
        circleStep: Int
    ) {
        val reached = step >= circleStep
        circle.setBackgroundResource(
            if (reached) R.drawable.bg_subscription_step_circle_active
            else R.drawable.bg_subscription_step_circle_inactive
        )
        circle.setTextColor(
            requireContext().getColor(if (reached) R.color.ds_on_primary else R.color.ds_text_secondary)
        )
        circle.text = if (step > circleStep) "✓" else circleStep.toString()
        label.setTextColor(requireContext().getColor(if (reached) R.color.ds_primary else R.color.ds_text_secondary))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val STEP_DETAILS = 1
        const val STEP_ITEMS = 2
        const val STEP_REVIEW = 3
    }
}
