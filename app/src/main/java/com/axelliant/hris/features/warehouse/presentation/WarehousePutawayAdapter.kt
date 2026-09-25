package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehousePutawayBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehousePutawayModel
import java.math.BigDecimal

class WarehousePutawayAdapter : RecyclerView.Adapter<WarehousePutawayAdapter.PutawayViewHolder>() {

    private val items = mutableListOf<WarehousePutawayModel>()

    fun submitList(newItems: List<WarehousePutawayModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PutawayViewHolder {
        val binding = ItemWarehousePutawayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PutawayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PutawayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PutawayViewHolder(
        private val binding: ItemWarehousePutawayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WarehousePutawayModel) = with(binding) {
            val context = root.context
            productText.text = item.productName.displayValue(context)
            quantityBadge.text = context.getString(
                R.string.putaway_quantity_badge_format,
                item.quantity.formatQuantity()
            )
            fromLocationText.text = item.fromLocationPath.displayValue(context)
            toLocationText.text = item.toLocationPath.displayValue(context)
            warehouseText.text = item.warehouseName.displayValue(context)
            sourceText.text = item.poNumber.displayValue(context)
            notesText.text = item.notes.displayValue(context)
            createdDateText.text = item.createdDate.displayValue(context)
        }

        private fun String.displayValue(context: Context): String {
            return takeIf { it.isNotBlank() && it != "-" }
                ?: context.getString(R.string.warehouse_not_available)
        }

        private fun Double.formatQuantity(): String {
            return BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
        }
    }
}
