package com.axelliant.hris.features.saleorders.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemSaleOrderProductLineBinding
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderProductLine

class SaleOrderProductLineAdapter :
    ListAdapter<SaleOrderProductLine, SaleOrderProductLineAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemSaleOrderProductLineBinding.inflate(
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
        private val binding: ItemSaleOrderProductLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaleOrderProductLine) {
            binding.productNameText.text = item.name
            binding.unitPriceText.text = item.unitPrice
            binding.skuText.text = binding.root.context.getString(
                R.string.sale_order_sku_format,
                item.sku
            )
            binding.qtyValue.text = item.quantity.toString()
            binding.lineTotalValue.text = item.lineTotal
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SaleOrderProductLine>() {
        override fun areItemsTheSame(
            oldItem: SaleOrderProductLine,
            newItem: SaleOrderProductLine
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SaleOrderProductLine,
            newItem: SaleOrderProductLine
        ): Boolean = oldItem == newItem
    }
}
