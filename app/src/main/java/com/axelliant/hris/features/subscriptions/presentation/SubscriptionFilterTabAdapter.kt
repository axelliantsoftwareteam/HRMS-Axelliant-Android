package com.axelliant.hris.features.subscriptions.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemSubscriptionFilterTabBinding

data class SubscriptionFilterTab(
    val id: String,
    @StringRes val labelRes: Int,
)

class SubscriptionFilterTabAdapter(
    private val onSelected: (SubscriptionFilterTab) -> Unit,
) : ListAdapter<SubscriptionFilterTab, SubscriptionFilterTabAdapter.TabViewHolder>(DiffCallback) {

    private var selectedId: String? = null

    fun submitTabs(tabs: List<SubscriptionFilterTab>, selectedTabId: String? = null) {
        selectedId = selectedTabId?.takeIf { id -> tabs.any { it.id == id } }
            ?: selectedId?.takeIf { id -> tabs.any { it.id == id } }
            ?: tabs.firstOrNull()?.id
        submitList(tabs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return TabViewHolder(
            ItemSubscriptionFilterTabBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        )
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TabViewHolder(
        private val binding: ItemSubscriptionFilterTabBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tab: SubscriptionFilterTab) = with(binding.filterTabLabel) {
            val selected = tab.id == selectedId
            setText(tab.labelRes)
            setBackgroundResource(
                if (selected) R.drawable.bg_subscription_filter_chip_selected
                else R.drawable.bg_subscription_filter_chip
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.ds_on_primary else R.color.ds_text_primary,
                )
            )
            setOnClickListener {
                if (selectedId == tab.id) return@setOnClickListener
                val previousId = selectedId
                selectedId = tab.id
                currentList.indexOfFirst { it.id == previousId }
                    .takeIf { it >= 0 }
                    ?.let(::notifyItemChanged)
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let(::notifyItemChanged)
                onSelected(tab)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SubscriptionFilterTab>() {
        override fun areItemsTheSame(oldItem: SubscriptionFilterTab, newItem: SubscriptionFilterTab) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SubscriptionFilterTab, newItem: SubscriptionFilterTab) =
            oldItem == newItem
    }
}
