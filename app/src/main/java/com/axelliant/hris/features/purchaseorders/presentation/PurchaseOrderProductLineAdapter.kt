package com.axelliant.hris.features.purchaseorders.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemPoDetailProductBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderProductLine

class PurchaseOrderProductLineAdapter :
    ListAdapter<PurchaseOrderProductLine, PurchaseOrderProductLineAdapter.ProductViewHolder>(
        DiffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemPoDetailProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemPoDetailProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PurchaseOrderProductLine) {
            val context = binding.root.context
            binding.productNameText.text = item.name
            binding.partNumberText.text = context.getString(
                R.string.po_details_part_no_format,
                item.partNumber
            )
            binding.qtyText.text = context.getString(
                R.string.po_details_qty_format,
                item.quantity
            )
            binding.unitPriceText.text = context.getString(
                R.string.po_details_unit_price_format,
                item.unitPrice
            )
            binding.lineTotalValue.text = item.lineTotal
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PurchaseOrderProductLine>() {
        override fun areItemsTheSame(
            oldItem: PurchaseOrderProductLine,
            newItem: PurchaseOrderProductLine
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PurchaseOrderProductLine,
            newItem: PurchaseOrderProductLine
        ): Boolean = oldItem == newItem
    }
}
