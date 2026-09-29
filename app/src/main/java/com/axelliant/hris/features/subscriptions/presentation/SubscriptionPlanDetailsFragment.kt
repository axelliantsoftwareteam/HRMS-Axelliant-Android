package com.axelliant.hris.features.subscriptions.presentation

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentSubscriptionPlanDetailsBinding
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanStatus

class SubscriptionPlanDetailsFragment : Fragment() {

    private var _binding: FragmentSubscriptionPlanDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionPlanDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val plan = readPlanArgument()
        if (plan == null) {
            Toast.makeText(requireContext(), R.string.subscription_plan_details_missing, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        setupInteractions()
        bindPlan(plan)
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.moreButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscriptions_more_actions_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.viewAllTiersButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_pricing_tiers, Toast.LENGTH_SHORT).show()
        }
        binding.editPlanButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_edit_plan, Toast.LENGTH_SHORT).show()
        }
        binding.createSubscriptionButton.setOnClickListener {
            findNavController().navigate(R.id.iaNewSubscriptionFragment)
        }
    }

    private fun bindPlan(plan: SubscriptionPlanModel) {
        val statusUi = plan.status.toStatusUi()
        val trialText = if (plan.allowTrial) {
            getString(R.string.subscription_detail_trial_days, DEFAULT_TRIAL_DAYS)
        } else {
            getString(R.string.subscription_no_trial)
        }

        binding.planNameText.text = plan.name
        binding.planCodeText.text = plan.code
        binding.priceText.text = plan.price
        binding.frequencyText.text = getString(
            R.string.subscription_detail_price_frequency,
            plan.billingFrequency.lowercase()
        )
        binding.billingModelText.text = plan.billingModel
        binding.statusBadge.text = getString(statusUi.labelRes)
        binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
        binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), statusUi.textColorRes))

        binding.trialChip.trialChipText.text = trialText
        binding.tierChip.tierChipText.text = resources.getQuantityString(
            R.plurals.subscription_detail_tier_count,
            plan.tierCount,
            plan.tierCount
        )
        binding.featureChip.featureChipText.text = resources.getQuantityString(
            R.plurals.subscription_detail_feature_count,
            plan.featureCount,
            plan.featureCount
        )
        binding.subscriptionChip.subscriptionChipText.text = resources.getQuantityString(
            R.plurals.subscription_detail_subscription_count,
            plan.activeSubscriptionCount,
            plan.activeSubscriptionCount
        )

        binding.billingSetting.billingFrequencyValue.text = plan.billingFrequency
        binding.currencySetting.currencyValue.text = plan.price.currencyCode()
        binding.trialSetting.trialPeriodValue.text = if (plan.allowTrial) {
            getString(R.string.subscription_detail_days_value, DEFAULT_TRIAL_DAYS)
        } else {
            getString(R.string.subscription_no_trial)
        }
        binding.statusSetting.statusValue.text = getString(statusUi.labelRes)
        binding.statusSetting.statusValue.setTextColor(ContextCompat.getColor(requireContext(), statusUi.textColorRes))

        bindTiers(plan)
        binding.usageCountText.text = plan.activeSubscriptionCount.toString()
        binding.usageDescriptionText.text = getString(R.string.subscription_detail_active_subscriptions)
        binding.usageCapacityText.text = getString(
            R.string.subscription_detail_capacity_used,
            plan.activeSubscriptionCount,
            DEFAULT_CAPACITY
        )
        binding.usageRemainingText.text = getString(
            R.string.subscription_detail_capacity_remaining,
            (DEFAULT_CAPACITY - plan.activeSubscriptionCount).coerceAtLeast(0)
        )
        binding.usageProgress.progress = plan.activeSubscriptionCount.coerceIn(0, DEFAULT_CAPACITY)
    }

    private fun bindTiers(plan: SubscriptionPlanModel) {
        val base = plan.price.amountValue()
        binding.tierStarter.starterPriceText.text = plan.price
        binding.tierGrowth.growthPriceText.text = plan.price.withAmount(base.multiply(GROWTH_MULTIPLIER))
        binding.tierScale.scalePriceText.text = plan.price.withAmount(base.multiply(SCALE_MULTIPLIER))
    }

    private fun readPlanArgument(): SubscriptionPlanModel? {
        val args = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_PLAN, SubscriptionPlanModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(ARG_PLAN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun SubscriptionPlanStatus.toStatusUi(): StatusUi {
        return when (this) {
            SubscriptionPlanStatus.ACTIVE -> StatusUi(
                R.string.subscription_status_active,
                R.drawable.bg_subscription_status_active,
                R.color.subscription_active_text
            )
            SubscriptionPlanStatus.DRAFT -> StatusUi(
                R.string.subscription_status_draft,
                R.drawable.bg_subscription_status_draft,
                R.color.subscription_draft_text
            )
            SubscriptionPlanStatus.ARCHIVED -> StatusUi(
                R.string.subscription_status_archived,
                R.drawable.bg_subscription_status_draft,
                R.color.subscription_draft_text
            )
            SubscriptionPlanStatus.UNKNOWN -> StatusUi(
                R.string.subscription_status_draft,
                R.drawable.bg_subscription_status_draft,
                R.color.subscription_draft_text
            )
        }
    }

    private fun String.currencyCode(): String = trim().substringBefore(" ").ifBlank { DEFAULT_CURRENCY }

    private fun String.amountValue() = trim()
        .split(Regex("\\s+"))
        .getOrNull(1)
        ?.toBigDecimalOrNull()
        ?: DEFAULT_AMOUNT.toBigDecimal()

    private fun String.withAmount(amount: java.math.BigDecimal): String {
        return "${currencyCode()} ${amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()}"
    }

    private data class StatusUi(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )

    companion object {
        const val ARG_PLAN = "subscription_plan"

        private const val DEFAULT_TRIAL_DAYS = 7
        private const val DEFAULT_CAPACITY = 100
        private const val DEFAULT_CURRENCY = "USD"
        private const val DEFAULT_AMOUNT = "20.00"
        private val GROWTH_MULTIPLIER = "0.90".toBigDecimal()
        private val SCALE_MULTIPLIER = "0.75".toBigDecimal()
    }
}
