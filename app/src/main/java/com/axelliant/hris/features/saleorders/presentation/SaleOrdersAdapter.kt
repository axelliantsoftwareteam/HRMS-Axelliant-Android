package com.axelliant.hris.features.saleorders.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.ItemSaleOrderBinding
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderModel

class SaleOrdersAdapter(
    private val onMenuClick: (SaleOrderModel, View) -> Unit
) : ListAdapter<SaleOrderModel, SaleOrdersAdapter.SaleOrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleOrderViewHolder {
        val binding = ItemSaleOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SaleOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SaleOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SaleOrderViewHolder(
        private val binding: ItemSaleOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: SaleOrderModel) {
            val context = binding.root.context
            val statusUi = SaleOrderStatusUiMapper.mapStatus(order.status)
            val utilizationUi = SaleOrderStatusUiMapper.mapUtilization(order.utilization)

            binding.orderNumberBadge.text = order.orderNumber
            binding.customerNameText.text = order.customerName
            binding.grandTotalValue.text = order.grandTotal
            binding.deliveryDateValue.text = order.deliveryDate

            binding.statusBadge.text = context.getString(statusUi.labelRes)
            binding.statusBadge.setBackgroundResource(statusUi.backgroundRes)
            binding.statusBadge.setTextColor(
                ContextCompat.getColor(context, statusUi.textColorRes)
            )

            binding.utilizationBadge.text = context.getString(utilizationUi.labelRes)
            binding.utilizationBadge.setBackgroundResource(utilizationUi.backgroundRes)
            binding.utilizationBadge.setTextColor(
                ContextCompat.getColor(context, utilizationUi.textColorRes)
            )

            binding.menuButton.setOnClickListener {
                onMenuClick(order, binding.menuButton)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SaleOrderModel>() {
        override fun areItemsTheSame(oldItem: SaleOrderModel, newItem: SaleOrderModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SaleOrderModel, newItem: SaleOrderModel): Boolean =
            oldItem == newItem
    }
}
