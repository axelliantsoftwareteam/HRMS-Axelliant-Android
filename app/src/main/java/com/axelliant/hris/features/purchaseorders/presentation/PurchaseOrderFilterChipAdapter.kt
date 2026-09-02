package com.axelliant.hris.features.purchaseorders.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemPurchaseOrderFilterChipBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilizationFilterChip

class PurchaseOrderFilterChipAdapter(
    private val onChipSelected: (PurchaseOrderUtilizationFilterChip) -> Unit
) : ListAdapter<PurchaseOrderUtilizationFilterChip, PurchaseOrderFilterChipAdapter.ChipViewHolder>(
    DiffCallback
) {

    private var selectedFilterId: String = FILTER_ALL_ID

    fun setSelectedFilterId(filterId: String) {
        if (selectedFilterId == filterId) return
        selectedFilterId = filterId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemPurchaseOrderFilterChipBinding.inflate(
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
        private val binding: ItemPurchaseOrderFilterChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PurchaseOrderUtilizationFilterChip) {
            val context = binding.root.context
            val isSelected = item.id == selectedFilterId
            binding.filterChipLabel.text = context.getString(
                R.string.purchase_orders_filter_chip_format,
                context.getString(item.labelRes),
                item.count
            )
            binding.filterChipLabel.setBackgroundResource(
                if (isSelected) {
                    R.drawable.bg_po_filter_selected
                } else {
                    R.drawable.bg_po_filter_default
                }
            )
            binding.filterChipLabel.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.ds_on_primary else R.color.purchase_order_fluent_text_secondary
                )
            )
            binding.filterChipLabel.typeface = ResourcesCompat.getFont(
                context,
                if (isSelected) R.font.poppins_semibold else R.font.poppins_medium
            )
            binding.root.setOnClickListener { onChipSelected(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PurchaseOrderUtilizationFilterChip>() {
        override fun areItemsTheSame(
            oldItem: PurchaseOrderUtilizationFilterChip,
            newItem: PurchaseOrderUtilizationFilterChip
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PurchaseOrderUtilizationFilterChip,
            newItem: PurchaseOrderUtilizationFilterChip
        ): Boolean = oldItem == newItem
    }

    companion object {
        const val FILTER_ALL_ID = "all"
    }
}
