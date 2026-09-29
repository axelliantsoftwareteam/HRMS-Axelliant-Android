package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehouseLocationBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationModel

class WarehouseLocationsAdapter(
    private val onMenuClick: (WarehouseLocationModel, View) -> Unit
) :
    RecyclerView.Adapter<WarehouseLocationsAdapter.LocationViewHolder>() {

    private val items = mutableListOf<WarehouseLocationModel>()

    fun submitList(newItems: List<WarehouseLocationModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemWarehouseLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding, onMenuClick)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LocationViewHolder(
        private val binding: ItemWarehouseLocationBinding,
        private val onMenuClick: (WarehouseLocationModel, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WarehouseLocationModel) = with(binding) {
            val context = root.context
            locationNameText.text = item.name.displayValue(context)
            warehouseText.text = item.warehouseName.displayValue(context)
            codeText.text = item.code.displayValue(context)
            levelText.text = item.levelTypeName.displayValue(context)
            defaultAreaText.setText(
                if (item.isDefaultArea) R.string.yes else R.string.no
            )
            pathText.text = item.path.displayValue(context)
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

        private fun String.displayValue(context: Context): String {
            return takeIf { it.isNotBlank() && it != "-" }
                ?: context.getString(R.string.warehouse_not_available)
        }
    }
}
