package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.ExpenseRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.expense.Expense

class MyExpenseAdapter(
    private val expenses: List<Expense>,
    private val context: Context,
    private val itemClick: AdapterItemClick,
) : RecyclerView.Adapter<MyExpenseAdapter.ExpenseVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseVH {
        val binding = ExpenseRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ExpenseVH(binding)
    }

    override fun onBindViewHolder(holder: ExpenseVH, position: Int) {
        val item = expenses[position]
        holder.bind(item, context)
        holder.binding.tvViewDetail.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClick.onItemClick(expenses[adapterPosition], adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = expenses.size

    class ExpenseVH(val binding: ExpenseRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Expense, context: Context) {
            binding.tvIdTxt.text = item.name.valueQualifier()
            binding.tvDateTxt.text = item.posting_date.valueQualifier()
            binding.tvStatus.text = item.displayStatus()
            binding.tvGrandTotalTxt.text = item.grand_total?.toString().valueQualifier()
            binding.tvPaidTxt.text = if (item.is_paid == 1) "Yes" else "No"
            binding.tvApprovedByTxt.text = item.expense_approver.valueQualifier()
            binding.tvViewDetail.text = "Edit"
            binding.tvDelete.isVisible = false
            applyStatusColors(item, context)
        }

        private fun Expense.displayStatus(): String {
            val statusValue = approval_status?.ifBlank { status.orEmpty() } ?: status.orEmpty()
            return if (statusValue == LeaveStatus.DRAFT.value) "Pending" else statusValue.valueQualifier()
        }

        private fun applyStatusColors(item: Expense, context: Context) {
            val statusValue = item.approval_status?.ifBlank { item.status.orEmpty() } ?: item.status.orEmpty()
            val (backgroundColor, textColor) = when (statusValue) {
                LeaveStatus.DRAFT.value, LeaveStatus.PENDING.value -> R.color.light_yellow to R.color.yellow
                LeaveStatus.APPROVED.value -> R.color.light_green to R.color.green
                LeaveStatus.REJECTED.value -> R.color.color_third_light to R.color.color_third
                else -> R.color.approved_bg to R.color.approved
            }
            binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(context, backgroundColor)
            binding.tvStatus.setTextColor(ContextCompat.getColorStateList(context, textColor))
        }
    }
}
