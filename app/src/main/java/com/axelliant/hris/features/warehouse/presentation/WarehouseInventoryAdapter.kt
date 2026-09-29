package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehouseInventoryBinding
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import java.util.Locale

class WarehouseInventoryAdapter(
    private val onMenuClick: (InventoryModel, View) -> Unit
) :
    RecyclerView.Adapter<WarehouseInventoryAdapter.InventoryViewHolder>() {

    private val items = mutableListOf<InventoryModel>()

    fun submitList(newItems: List<InventoryModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemWarehouseInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryViewHolder(binding, onMenuClick)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InventoryViewHolder(
        private val binding: ItemWarehouseInventoryBinding,
        private val onMenuClick: (InventoryModel, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryModel) = with(binding) {
            val context = root.context
            productNameText.text = item.productName.displayValue(context)
            sourcePoText.text = item.sourcePurchaseOrderNumber.displayValue(context)
            warehouseText.text = item.warehouseName.displayValue(context)
            locationText.text = item.locationPath.displayValue(context)
            serialTrackedText.setText(if (item.isSerialTracked) R.string.yes else R.string.no)
            onHandText.text = item.quantityOnHand.formatQuantity()
            reservedText.text = item.quantityReserved.formatQuantity()
            availableText.text = item.quantityAvailable.formatQuantity()
            createdDateText.text = item.createdDate.displayValue(context)
            statusBadge.text = item.balanceStatus.displayValue(context)
            tintStatusBadge(item.quantityAvailable)
            menuButton.setOnClickListener { anchor -> onMenuClick(item, anchor) }
        }

        private fun ItemWarehouseInventoryBinding.tintStatusBadge(quantityAvailable: Double) {
            val context = root.context
            val hasAvailableStock = quantityAvailable > 0.0
            statusBadge.setBackgroundResource(
                if (hasAvailableStock) {
                    R.drawable.bg_quote_status_approved
                } else {
                    R.drawable.bg_quote_status_submitted
                }
            )
            statusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (hasAvailableStock) {
                        R.color.quotes_status_approved_bg
                    } else {
                        R.color.quotes_status_submitted_bg
                    }
                )
            )
            statusBadge.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (hasAvailableStock) {
                        R.color.quotes_status_approved_text
                    } else {
                        R.color.quotes_status_submitted_text
                    }
                )
            )
        }

        private fun String.displayValue(context: Context): String {
            return takeIf { it.isNotBlank() && it != "-" }
                ?: context.getString(R.string.warehouse_not_available)
        }

        private fun Double.formatQuantity(): String {
            return if (this % 1.0 == 0.0) {
                toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", this)
            }
        }
    }
}
