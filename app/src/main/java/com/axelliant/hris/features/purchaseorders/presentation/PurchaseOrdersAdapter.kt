package com.axelliant.hris.features.purchaseorders.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.ItemPurchaseOrderBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel

class PurchaseOrdersAdapter(
    private val onMenuClick: (PurchaseOrderModel, View) -> Unit
) : ListAdapter<PurchaseOrderModel, PurchaseOrdersAdapter.PurchaseOrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseOrderViewHolder {
        val binding = ItemPurchaseOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PurchaseOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PurchaseOrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PurchaseOrderViewHolder(
        private val binding: ItemPurchaseOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: PurchaseOrderModel) {
            val context = binding.root.context
            val statusUi = PurchaseOrderStatusUiMapper.mapStatus(order.status)
            val utilizationUi = PurchaseOrderStatusUiMapper.mapUtilization(order.utilization)

            binding.poNumberBadge.text = order.poNumber
            binding.vendorNameText.text = order.vendorName
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

    private object DiffCallback : DiffUtil.ItemCallback<PurchaseOrderModel>() {
        override fun areItemsTheSame(
            oldItem: PurchaseOrderModel,
            newItem: PurchaseOrderModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PurchaseOrderModel,
            newItem: PurchaseOrderModel
        ): Boolean = oldItem == newItem
    }
}
