package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehouseReceivingBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptModel

class WarehouseReceivingAdapter(
    private val onMenuClick: (WarehouseReceiptModel, View) -> Unit
) : RecyclerView.Adapter<WarehouseReceivingAdapter.ReceivingViewHolder>() {

    private val items = mutableListOf<WarehouseReceiptModel>()

    fun submitList(newItems: List<WarehouseReceiptModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceivingViewHolder {
        val binding = ItemWarehouseReceivingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceivingViewHolder(binding, onMenuClick)
    }

    override fun onBindViewHolder(holder: ReceivingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ReceivingViewHolder(
        private val binding: ItemWarehouseReceivingBinding,
        private val onMenuClick: (WarehouseReceiptModel, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WarehouseReceiptModel) = with(binding) {
            val context = root.context
            receiptNumberText.text = item.receiptNumber.displayValue(context)
            typeText.text = item.receiptTypeName.displayValue(context)
            warehouseText.text = item.warehouseName.displayValue(context)
            sourceText.text = item.poNumber.displayValue(context)
            receivedDateText.text = item.receivedDate.displayValue(context)
            createdDateText.text = item.createdDate.displayValue(context)
            statusBadge.text = item.receiptStatusName.displayValue(context)
            tintStatusBadge(item.receiptStatus)
            menuButton.setOnClickListener { anchor -> onMenuClick(item, anchor) }
        }

        private fun ItemWarehouseReceivingBinding.tintStatusBadge(status: Int?) {
            val context = root.context
            val isComplete = status == COMPLETED_STATUS
            statusBadge.setBackgroundResource(
                if (isComplete) R.drawable.bg_quote_status_approved else R.drawable.bg_quote_status_submitted
            )
            statusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isComplete) R.color.quotes_status_approved_bg else R.color.quotes_status_submitted_bg
                )
            )
            statusBadge.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isComplete) R.color.quotes_status_approved_text else R.color.quotes_status_submitted_text
                )
            )
        }

        private fun String.displayValue(context: Context): String {
            return takeIf { it.isNotBlank() && it != "-" }
                ?: context.getString(R.string.warehouse_not_available)
        }
    }

    private companion object {
        const val COMPLETED_STATUS = 7
    }
}
