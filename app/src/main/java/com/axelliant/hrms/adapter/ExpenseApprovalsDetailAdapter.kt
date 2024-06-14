package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyTeamExpenseApprovalRowBinding
import com.axelliant.hrms.extention.nullToEmpty
import com.axelliant.hrms.model.expense.Expense
import com.axelliant.hrms.utils.Utils.hideShow

class ExpenseApprovalsDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<Expense>,
    private val approvedItemClick: AdapterItemClick,
    private val rejectItemClick: AdapterItemClick,
) :
    RecyclerView.Adapter<ExpenseApprovalsDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamExpenseApprovalRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(detailArrayList[position], mContext)

        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            detailArrayList[position].expenses_detail?.let { ExpenseRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.divider.isVisible = holder.binding.lyAttendStatus.isVisible
        }

        holder.binding.tvApproved.setOnClickListener{
            approvedItemClick.onItemClick(detailArrayList[position],position)
        }

        holder.binding.tvReject.setOnClickListener{
            rejectItemClick.onItemClick(detailArrayList[position],position)

        }

    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamExpenseApprovalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Expense, mContext: Context)
        {
            binding.tvEmployeName.text = item.employee_name
            binding.tvEmployeDesignation.text = item.department

        }
    }

}