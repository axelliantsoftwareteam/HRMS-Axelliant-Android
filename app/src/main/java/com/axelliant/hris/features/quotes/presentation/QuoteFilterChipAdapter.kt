package com.axelliant.hris.features.quotes.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemQuoteFilterChipBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteFilterChip
import com.axelliant.hris.features.quotes.domain.model.QuoteStatusFilterType

class QuoteFilterChipAdapter(
    private val onChipSelected: (QuoteFilterChip) -> Unit
) : ListAdapter<QuoteFilterChip, QuoteFilterChipAdapter.ChipViewHolder>(DiffCallback) {

    private var selectedFilterId: String = QuoteStatusFilterType.ALL.filterId

    fun setSelectedFilterId(filterId: String) {
        if (selectedFilterId == filterId) return
        selectedFilterId = filterId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemQuoteFilterChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChipViewHolder(
        private val binding: ItemQuoteFilterChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuoteFilterChip) {
            val context = binding.root.context
            val isSelected = item.filterId == selectedFilterId
            val label = item.label(context)
            binding.filterChipLabel.text = context.getString(
                R.string.quotes_filter_chip_format,
                label,
                item.count
            )
            binding.filterChipLabel.setBackgroundResource(
                if (isSelected) {
                    R.drawable.bg_quote_filter_chip_selected
                } else {
                    R.drawable.bg_quote_filter_chip_default
                }
            )
            binding.filterChipLabel.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.ia_white else R.color.quotes_filter_chip_default_text
                )
            )
            binding.filterChipLabel.typeface = ResourcesCompat.getFont(
                context,
                if (isSelected) R.font.poppins_semibold else R.font.poppins_medium
            )
            binding.root.setOnClickListener { onChipSelected(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuoteFilterChip>() {
        override fun areItemsTheSame(
            oldItem: QuoteFilterChip,
            newItem: QuoteFilterChip
        ): Boolean = oldItem.status == newItem.status

        override fun areContentsTheSame(
            oldItem: QuoteFilterChip,
            newItem: QuoteFilterChip
        ): Boolean = oldItem == newItem
    }

}
