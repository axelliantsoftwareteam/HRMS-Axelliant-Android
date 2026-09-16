package com.axelliant.hris.features.purchaseorders.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemPurchaseOrderHistoryBinding
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderHistoryItemUiModel

class PurchaseOrderHistoryAdapter :
    ListAdapter<PurchaseOrderHistoryItemUiModel, PurchaseOrderHistoryAdapter.HistoryViewHolder>(
        DiffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemPurchaseOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(
            item = getItem(position),
            isLastItem = position == itemCount - 1
        )
    }

    class HistoryViewHolder(
        private val binding: ItemPurchaseOrderHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PurchaseOrderHistoryItemUiModel, isLastItem: Boolean) {
            val context = binding.root.context
            val style = resolveStyle(item)

            binding.processNameText.text = item.processName
            binding.roleNameText.text = item.roleName
            binding.commentText.text = context.getString(
                R.string.purchase_order_history_comment_format,
                item.comment.ifBlank {
                    context.getString(R.string.purchase_order_history_no_comment)
                }
            )
            binding.dateTimeText.text = context.getString(
                R.string.purchase_order_history_date_time_format,
                item.dateText,
                item.timeText
            )
            binding.iconContainer.setBackgroundResource(style.backgroundRes)
            binding.processIcon.setImageResource(style.iconRes)
            binding.processIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, style.colorRes)
            )
            binding.statusDot.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, style.colorRes)
            )
            binding.timelineLine.alpha = if (isLastItem) 0.35f else 1f
        }

        private fun resolveStyle(item: PurchaseOrderHistoryItemUiModel): HistoryStyle {
            val name = item.processName.lowercase()
            return when {
                "send" in name || ("vendor" in name && "confirm" !in name) -> HistoryStyle(
                    iconRes = R.drawable.ic_po_history_send,
                    backgroundRes = R.drawable.bg_po_history_icon_teal,
                    colorRes = R.color.ds_success
                )
                "confirm" in name || item.status -> HistoryStyle(
                    iconRes = R.drawable.ic_check_circle,
                    backgroundRes = R.drawable.bg_po_history_icon_green,
                    colorRes = R.color.ds_success
                )
                "review" in name -> HistoryStyle(
                    iconRes = R.drawable.ic_person_check,
                    backgroundRes = R.drawable.bg_po_history_icon_orange,
                    colorRes = R.color.ds_pending
                )
                else -> HistoryStyle(
                    iconRes = R.drawable.ic_document,
                    backgroundRes = R.drawable.bg_po_history_icon_blue,
                    colorRes = R.color.ds_primary
                )
            }
        }
    }

    private data class HistoryStyle(
        val iconRes: Int,
        val backgroundRes: Int,
        val colorRes: Int
    )

    private object DiffCallback : DiffUtil.ItemCallback<PurchaseOrderHistoryItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: PurchaseOrderHistoryItemUiModel,
            newItem: PurchaseOrderHistoryItemUiModel
        ): Boolean = oldItem.processNo == newItem.processNo &&
            oldItem.commentedOn == newItem.commentedOn

        override fun areContentsTheSame(
            oldItem: PurchaseOrderHistoryItemUiModel,
            newItem: PurchaseOrderHistoryItemUiModel
        ): Boolean = oldItem == newItem
    }
}
