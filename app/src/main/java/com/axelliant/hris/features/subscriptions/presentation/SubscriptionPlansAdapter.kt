package com.axelliant.hris.features.subscriptions.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemSubscriptionPlanBusinessBinding
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanStatus

class SubscriptionPlansAdapter :
    ListAdapter<SubscriptionPlanModel, SubscriptionPlansAdapter.PlanViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemSubscriptionPlanBusinessBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlanViewHolder(
        private val binding: ItemSubscriptionPlanBusinessBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plan: SubscriptionPlanModel) {
            val context = binding.root.context
            val statusUi = plan.status.toStatusUi()

            binding.planNameText.text = plan.name
            binding.planCodeText.text = plan.code
            binding.priceText.text = context.getString(
                R.string.subscription_plan_price_format,
                plan.price,
                plan.billingFrequency.lowercase()
            )
            binding.billingModelText.text = context.getString(
                R.string.subscription_plan_billing_model_format,
                plan.billingModel
            )
            binding.metaText.text = context.getString(
                R.string.subscription_plan_meta_format,
                if (plan.allowTrial) {
                    context.getString(R.string.subscription_trial_available)
                } else {
                    context.getString(R.string.subscription_no_trial)
                },
                plan.tierCount,
                plan.featureCount,
                plan.activeSubscriptionCount
            )
            binding.statusBadge.text = context.getString(statusUi.labelRes)
            binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
            binding.statusBadge.setTextColor(ContextCompat.getColor(context, statusUi.textColorRes))
        }

        private fun SubscriptionPlanStatus.toStatusUi(): PlanStatusUi {
            return when (this) {
                SubscriptionPlanStatus.ACTIVE -> PlanStatusUi(
                    R.string.subscription_status_active,
                    R.drawable.bg_subscription_status_active,
                    R.color.subscription_active_text
                )
                SubscriptionPlanStatus.DRAFT -> PlanStatusUi(
                    R.string.subscription_status_draft,
                    R.drawable.bg_subscription_status_draft,
                    R.color.subscription_draft_text
                )
                SubscriptionPlanStatus.ARCHIVED -> PlanStatusUi(
                    R.string.subscription_status_archived,
                    R.drawable.bg_subscription_status_draft,
                    R.color.subscription_draft_text
                )
                SubscriptionPlanStatus.UNKNOWN -> PlanStatusUi(
                    R.string.subscription_status_draft,
                    R.drawable.bg_subscription_status_draft,
                    R.color.subscription_draft_text
                )
            }
        }
    }

    private data class PlanStatusUi(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )

    private object DiffCallback : DiffUtil.ItemCallback<SubscriptionPlanModel>() {
        override fun areItemsTheSame(
            oldItem: SubscriptionPlanModel,
            newItem: SubscriptionPlanModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SubscriptionPlanModel,
            newItem: SubscriptionPlanModel
        ): Boolean = oldItem == newItem
    }
}
