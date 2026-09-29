package com.axelliant.hris.features.saleorders.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemQuoteFilterChipBinding
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderFilterChip
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatusFilterType

class SaleOrderFilterChipAdapter(
    private val onChipSelected: (SaleOrderFilterChip) -> Unit
) : ListAdapter<SaleOrderFilterChip, SaleOrderFilterChipAdapter.ChipViewHolder>(DiffCallback) {

    private var selectedFilterId: String = SaleOrderStatusFilterType.ALL.filterId

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

        fun bind(item: SaleOrderFilterChip) {
            val context = binding.root.context
            val isSelected = item.filterId == selectedFilterId
            binding.filterChipLabel.text = context.getString(
                R.string.sale_orders_filter_chip_format,
                item.label(context),
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

    private object DiffCallback : DiffUtil.ItemCallback<SaleOrderFilterChip>() {
        override fun areItemsTheSame(
            oldItem: SaleOrderFilterChip,
            newItem: SaleOrderFilterChip
        ): Boolean = oldItem.filterType == newItem.filterType

        override fun areContentsTheSame(
            oldItem: SaleOrderFilterChip,
            newItem: SaleOrderFilterChip
        ): Boolean = oldItem == newItem
    }
}
