package com.axelliant.hris.features.subscriptions.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemSubscriptionFilterTabBinding

data class SubscriptionFilterTab(
    val id: String,
    @StringRes val labelRes: Int,
    val count: Int = 0,
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
            text = context.getString(tab.labelRes, tab.count)
            setBackgroundResource(
                if (selected) R.drawable.bg_quote_filter_chip_selected
                else R.drawable.bg_quote_filter_chip_default
            )

            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.ia_white else R.color.quotes_filter_chip_default_text,
                )
            )
            ResourcesCompat.getFont(
                context,
                if (isSelected) R.font.poppins_semibold else R.font.poppins_medium
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
