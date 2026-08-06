package com.axelliant.hris.features.quotes.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.ItemQuoteBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteModel

class QuotesAdapter(
    private val listener: QuoteItemListener
) : ListAdapter<QuoteModel, QuotesAdapter.QuoteViewHolder>(DiffCallback) {

    interface QuoteItemListener {
        fun onMenuClick(quote: QuoteModel, anchor: android.view.View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(
        private val binding: ItemQuoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: QuoteModel) {
            val context = binding.root.context
            val statusUi = QuoteStatusUiMapper.getStatusUi(quote.approvalStatus)

            binding.quoteIdText.text = quote.quoteId
            binding.customerNameText.text = quote.customerName
            binding.createdByValue.text = quote.createdBy
            binding.dateValue.text = quote.date
            binding.totalAmountValue.text = quote.totalAmount
            binding.statusBadge.text = context.getString(statusUi.labelRes)
            binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
            binding.statusBadge.setTextColor(
                ContextCompat.getColor(context, statusUi.textColorRes)
            )
            binding.menuButton.setOnClickListener {
                listener.onMenuClick(quote, binding.menuButton)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuoteModel>() {
        override fun areItemsTheSame(oldItem: QuoteModel, newItem: QuoteModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: QuoteModel, newItem: QuoteModel): Boolean =
            oldItem == newItem
    }
}
