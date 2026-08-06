package com.axelliant.hris.features.quotes.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemQuotePreviewProductBinding
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewProductUiModel

class QuotePreviewProductAdapter :
    ListAdapter<QuotePreviewProductUiModel, QuotePreviewProductAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemQuotePreviewProductBinding.inflate(
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
        private val binding: ItemQuotePreviewProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuotePreviewProductUiModel) {
            val context = binding.root.context
            binding.productSkuText.text = context.getString(R.string.quote_preview_sku_format, item.sku)
            binding.productQtyText.text = context.getString(R.string.quote_preview_qty_format, item.quantity)
            binding.productNameText.text = item.productName
            binding.productUnitText.text = context.getString(R.string.quote_preview_unit_format, item.unitPrice)
            binding.productLineTotalText.text = item.lineTotal
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuotePreviewProductUiModel>() {
        override fun areItemsTheSame(
            oldItem: QuotePreviewProductUiModel,
            newItem: QuotePreviewProductUiModel
        ): Boolean = oldItem.sku == newItem.sku && oldItem.quantity == newItem.quantity

        override fun areContentsTheSame(
            oldItem: QuotePreviewProductUiModel,
            newItem: QuotePreviewProductUiModel
        ): Boolean = oldItem == newItem
    }
}
