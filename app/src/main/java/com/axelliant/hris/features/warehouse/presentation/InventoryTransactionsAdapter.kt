package com.axelliant.hris.features.warehouse.presentation

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemInventoryTransactionBinding
import com.axelliant.hris.features.warehouse.domain.model.InventoryTransactionModel
import java.util.Locale

class InventoryTransactionsAdapter :
    ListAdapter<InventoryTransactionModel, InventoryTransactionsAdapter.TransactionViewHolder>(
        DiffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemInventoryTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemInventoryTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryTransactionModel) = with(binding) {
            val context = root.context
            typeBadge.text = item.transactionTypeName.displayValue(context)
            quantityText.text = item.quantity.formatQuantity()
            createdDateText.text = item.createdDate.displayValue(context)
            fromLocationText.text = item.fromLocationPath.displayValue(context)
            toLocationText.text = item.toLocationPath.displayValue(context)
            referenceText.text = item.referenceType.displayValue(context)
            notesText.text = item.notes.displayValue(context)
            notesRow.isVisible = item.notes.isNotBlank()
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

    private object DiffCallback : DiffUtil.ItemCallback<InventoryTransactionModel>() {
        override fun areItemsTheSame(
            oldItem: InventoryTransactionModel,
            newItem: InventoryTransactionModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: InventoryTransactionModel,
            newItem: InventoryTransactionModel
        ): Boolean = oldItem == newItem
    }
}
