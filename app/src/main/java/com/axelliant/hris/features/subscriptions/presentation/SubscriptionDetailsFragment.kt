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
import com.axelliant.hris.databinding.FragmentSubscriptionDetailsBinding
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.math.RoundingMode

class SubscriptionDetailsFragment : Fragment() {

    private var _binding: FragmentSubscriptionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val subscription = readSubscriptionArgument()
        if (subscription == null) {
            Toast.makeText(requireContext(), R.string.subscription_details_missing, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupInteractions()
        bindSubscription(subscription)
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.moreButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscriptions_more_actions_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.viewAccountAction.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_view_account, Toast.LENGTH_SHORT).show()
        }
        binding.addItemButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_add_item, Toast.LENGTH_SHORT).show()
        }
        binding.itemRow.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_items, Toast.LENGTH_SHORT).show()
        }
        binding.additionalDetailsHeader.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_additional_details, Toast.LENGTH_SHORT).show()
        }
        binding.editSubscriptionButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_edit_subscription, Toast.LENGTH_SHORT).show()
        }
        binding.viewInvoicesButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.subscription_view_invoices, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSubscription(subscription: SubscriptionModel) {
        val statusUi = subscription.status.toStatusUi()
        val unitPrice = subscription.price.unitPrice()
        val discount = subscription.price.discount()
        val taxRate = DEFAULT_TAX_RATE
        val monthlyTotal = subscription.price

        binding.subscriptionNumberText.text = subscription.subscriptionNumber
        binding.accountNameText.text = subscription.accountName
        binding.planNameText.text = subscription.planName
        binding.priceText.text = monthlyTotal
        binding.statusBadge.text = getString(statusUi.labelRes)
        binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
        binding.statusBadge.setTextColor(ContextCompat.getColor(requireContext(), statusUi.textColorRes))

        binding.lifecycleStarted.startedDateValue.text = STARTED_DATE
        binding.lifecycleNextBilling.nextBillingDateValue.text = subscription.nextBillingDate
        binding.lifecycleAutoRenew.autoRenewValue.text = if (subscription.autoRenew) {
            getString(R.string.subscription_auto_renew_on_short)
        } else {
            getString(R.string.subscription_auto_renew_off_short)
        }

        binding.termsUnitPrice.unitPriceValue.text = unitPrice
        binding.termsQuantity.quantityValue.text = DEFAULT_QUANTITY
        binding.termsDiscount.discountValue.text = discount
        binding.termsTax.taxRateValue.text = getString(R.string.subscription_tax_rate_value, taxRate)
        binding.monthlyTotalValue.text = monthlyTotal

        binding.itemCountText.text = getString(R.string.subscription_items_count, 1)
        binding.itemTitleText.text = getString(R.string.subscription_item_license_title)
        binding.itemSubtitleText.text = getString(R.string.subscription_item_base_plan)
        binding.itemQuantityValue.text = DEFAULT_QUANTITY
        binding.itemUnitPriceValue.text = unitPrice

        binding.billingFrequency.billingFrequencyValue.text = getString(R.string.subscription_frequency_monthly)
        binding.billingCurrency.currencyValue.text = subscription.price.currencyCode()
        binding.billingInvoice.nextInvoiceValue.text = subscription.nextBillingDate
    }

    private fun readSubscriptionArgument(): SubscriptionModel? {
        val args = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_SUBSCRIPTION, SubscriptionModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(ARG_SUBSCRIPTION)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun SubscriptionStatus.toStatusUi(): StatusUi {
        return when (this) {
            SubscriptionStatus.ACTIVE -> StatusUi(
                R.string.subscription_status_active,
                R.drawable.bg_subscription_status_active,
                R.color.subscription_active_text
            )
            SubscriptionStatus.TRIAL -> StatusUi(
                R.string.subscription_status_trial,
                R.drawable.bg_subscription_status_trial,
                R.color.ds_primary
            )
            SubscriptionStatus.PAST_DUE -> StatusUi(
                R.string.subscription_status_past_due,
                R.drawable.bg_subscription_status_past_due,
                R.color.ds_error
            )
            SubscriptionStatus.DRAFT,
            SubscriptionStatus.UNKNOWN -> StatusUi(
                R.string.subscription_status_draft,
                R.drawable.bg_subscription_status_draft,
                R.color.subscription_draft_text
            )
        }
    }

    private fun String.currencyCode(): String {
        return trim().substringBefore(" ").ifBlank { DEFAULT_CURRENCY }
    }

    private fun String.unitPrice(): String {
        val parts = trim().split(Regex("\\s+"))
        val total = parts.getOrNull(1)?.toBigDecimalOrNull()
            ?: return this
        val subtotalBeforeTax = total
            .multiply(BigDecimal(100))
            .divide(BigDecimal(109), 2, RoundingMode.HALF_UP)
        val unitAmount = subtotalBeforeTax
            .add(DISCOUNT_AMOUNT)
            .divide(BigDecimal(DEFAULT_QUANTITY), 2, RoundingMode.HALF_UP)
        return "${currencyCode()} ${unitAmount.toPlainString()}"
    }

    private fun String.discount(): String {
        return "${currencyCode()} ${DISCOUNT_AMOUNT.toPlainString()}"
    }

    private data class StatusUi(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )

    companion object {
        const val ARG_SUBSCRIPTION = "subscription"

        private const val STARTED_DATE = "Aug 19, 2026"
        private const val DEFAULT_CURRENCY = "USD"
        private const val DEFAULT_QUANTITY = "2"
        private const val DEFAULT_TAX_RATE = "9%"
        private val DISCOUNT_AMOUNT = "10.00".toBigDecimal()
    }
}
