package com.axelliant.hris.features.subscriptions.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemSubscriptionCardActiveBinding
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionStatus

class SubscriptionsAdapter :
    ListAdapter<SubscriptionModel, SubscriptionsAdapter.SubscriptionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val binding = ItemSubscriptionCardActiveBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SubscriptionViewHolder(
        private val binding: ItemSubscriptionCardActiveBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subscription: SubscriptionModel) {
            val context = binding.root.context
            val statusUi = subscription.status.toStatusUi()

            binding.subscriptionNumberText.text = subscription.subscriptionNumber
            binding.accountNameText.text = subscription.accountName
            binding.planNameText.text = subscription.planName
            binding.priceText.text = subscription.price
            binding.nextBillingDateText.text = subscription.nextBillingDate
            binding.autoRenewText.text = context.getString(
                if (subscription.autoRenew) {
                    R.string.subscription_auto_renew_on
                } else {
                    R.string.subscription_auto_renew_off
                }
            )
            binding.statusBadge.text = context.getString(statusUi.labelRes)
            binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
            binding.statusBadge.setTextColor(ContextCompat.getColor(context, statusUi.textColorRes))
        }

        private fun SubscriptionStatus.toStatusUi(): SubscriptionStatusUi {
            return when (this) {
                SubscriptionStatus.ACTIVE -> SubscriptionStatusUi(
                    R.string.subscription_status_active,
                    R.drawable.bg_subscription_status_active,
                    R.color.subscription_active_text
                )
                SubscriptionStatus.TRIAL -> SubscriptionStatusUi(
                    R.string.subscription_status_trial,
                    R.drawable.bg_subscription_status_trial,
                    R.color.ds_primary
                )
                SubscriptionStatus.PAST_DUE -> SubscriptionStatusUi(
                    R.string.subscription_status_past_due,
                    R.drawable.bg_subscription_status_past_due,
                    R.color.ds_error
                )
                SubscriptionStatus.DRAFT -> SubscriptionStatusUi(
                    R.string.subscription_status_draft,
                    R.drawable.bg_subscription_status_draft,
                    R.color.subscription_draft_text
                )
                SubscriptionStatus.UNKNOWN -> SubscriptionStatusUi(
                    R.string.subscription_status_draft,
                    R.drawable.bg_subscription_status_draft,
                    R.color.subscription_draft_text
                )
            }
        }
    }

    private data class SubscriptionStatusUi(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )

    private object DiffCallback : DiffUtil.ItemCallback<SubscriptionModel>() {
        override fun areItemsTheSame(
            oldItem: SubscriptionModel,
            newItem: SubscriptionModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SubscriptionModel,
            newItem: SubscriptionModel
        ): Boolean = oldItem == newItem
    }
}
