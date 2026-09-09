package com.axelliant.hris.features.warehouse.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehouseBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel

class WarehousesAdapter(
    private val onMenuClick: (WarehouseModel, View) -> Unit
) : RecyclerView.Adapter<WarehousesAdapter.WarehouseViewHolder>() {

    private val items = mutableListOf<WarehouseModel>()

    fun submitList(newItems: List<WarehouseModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarehouseViewHolder {
        val binding = ItemWarehouseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WarehouseViewHolder(binding, onMenuClick)
    }

    override fun onBindViewHolder(holder: WarehouseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class WarehouseViewHolder(
        private val binding: ItemWarehouseBinding,
        private val onMenuClick: (WarehouseModel, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WarehouseModel) = with(binding) {
            val context = root.context
            warehouseCodeText.text = item.name.displayValue(context)
            warehouseNameText.text = item.code.displayValue(context)
            cityText.text = item.city.displayValue(context)
            stateText.text = item.state.displayValue(context)
            countryText.text = item.country.displayValue(context)
            createdDateText.text = item.createdDate.displayValue(context)
            statusBadge.setText(
                if (item.isActive) R.string.warehouse_status_active else R.string.warehouse_status_inactive
            )
            statusBadge.setBackgroundResource(
                if (item.isActive) R.drawable.bg_quote_status_approved else R.drawable.bg_quote_status_rejected
            )
            statusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.isActive) R.color.quotes_status_approved_bg else R.color.quotes_status_rejected_bg
                )
            )
            statusBadge.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.isActive) R.color.quotes_status_approved_text else R.color.quotes_status_rejected_text
                )
            )
            menuButton.setOnClickListener { anchor -> onMenuClick(item, anchor) }
        }

        private fun String.displayValue(context: android.content.Context): String {
            return takeIf { it.isNotBlank() && it != "-" }
                ?: context.getString(R.string.warehouse_not_available)
        }
    }
}
