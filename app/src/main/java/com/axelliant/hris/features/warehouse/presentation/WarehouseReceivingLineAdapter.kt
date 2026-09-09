package com.axelliant.hris.features.warehouse.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemWarehouseReceivingLineBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptLineModel
import java.text.DecimalFormat

class WarehouseReceivingLineAdapter :
    ListAdapter<WarehouseReceiptLineModel, WarehouseReceivingLineAdapter.LineViewHolder>(
        DiffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemWarehouseReceivingLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LineViewHolder(
        private val binding: ItemWarehouseReceivingLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WarehouseReceiptLineModel) = with(binding) {
            val context = root.context
            productNameText.text = item.productName.displayValue()
            productCodeText.text = context.getString(
                R.string.receiving_detail_product_code_format,
                item.productCode.displayValue()
            )
            uomField.fieldLabel.text = context.getString(R.string.receiving_detail_uom)
            uomField.fieldValue.text = item.uom.displayValue()
            expectedQtyField.fieldLabel.text = context.getString(R.string.receiving_detail_expected_qty)
            expectedQtyField.fieldValue.text = item.expectedQuantity.formatQuantity()
            receivedQtyField.fieldLabel.text = context.getString(R.string.receiving_detail_received_qty)
            receivedQtyField.fieldValue.text = item.receivedQuantity.formatQuantity()
            putawayQtyField.fieldLabel.text = context.getString(R.string.receiving_detail_putaway_qty)
            putawayQtyField.fieldValue.text = item.quantityPutaway.formatQuantity()
            remainingPutawayField.fieldLabel.text = context.getString(R.string.receiving_detail_remaining_putaway)
            remainingPutawayField.fieldValue.text = item.quantityRemainingToPutaway.formatQuantity()
            serialTrackedText.isVisible = item.isSerialTracked
            inspectionStatusBadge.text = item.inspectionStatusName.displayValue()
            actionStatusBadge.text = item.actionStatusName.displayValue()
            tintBadge(inspectionStatusBadge, item.inspectionStatusName)
            tintBadge(actionStatusBadge, item.actionStatusName)
        }

        private fun tintBadge(
            view: com.axelliant.hris.ui.designsystem.components.AppTextView,
            label: String
        ) {
            val context = binding.root.context
            val isPositive = label.equals("Accepted", ignoreCase = true) ||
                label.equals("Completed", ignoreCase = true)
            view.setBackgroundResource(
                if (isPositive) R.drawable.bg_quote_status_approved else R.drawable.bg_quote_status_submitted
            )
            view.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isPositive) R.color.quotes_status_approved_bg else R.color.quotes_status_submitted_bg
                )
            )
            view.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isPositive) R.color.quotes_status_approved_text else R.color.quotes_status_submitted_text
                )
            )
        }

        private fun String.displayValue(): String = takeIf { it.isNotBlank() && it != "-" } ?: "N/A"

        private fun Double.formatQuantity(): String {
            return if (this % 1.0 == 0.0) {
                toInt().toString()
            } else {
                DecimalFormat("0.##").format(this)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<WarehouseReceiptLineModel>() {
        override fun areItemsTheSame(
            oldItem: WarehouseReceiptLineModel,
            newItem: WarehouseReceiptLineModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: WarehouseReceiptLineModel,
            newItem: WarehouseReceiptLineModel
        ): Boolean = oldItem == newItem
    }
}
