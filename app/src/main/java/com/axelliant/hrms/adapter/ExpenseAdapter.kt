package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyTeamExpenseRowBinding
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.model.expense.Expense
import com.axelliant.hrms.model.expense.ExpenseStatu
import com.axelliant.hrms.utils.Utils.hideShow

class ExpenseAdapter(
    private val list: ArrayList<Expense>,
    private val mContext: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<ExpenseAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamExpenseRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], mContext)

//        holder.binding.tvViewDetail.setOnClickListener {
//            itemClick.onItemClick(list[position], position)
//        }
        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 1)
        holder.binding.rvLeaveCount.adapter =
            list[position].expenses_detail?.let { ExpenseRowAdapter(it, mContext) }
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false


        holder.binding.lyWeekly.setOnClickListener {


            itemClick.onItemClick(list[position], position)
        }

        holder.binding.icDropDown.setOnClickListener {
            holder.binding.lyDropDown.hideShow(it)
            holder.binding.lineDiv.isVisible = holder.binding.rvLeaveCount.isVisible

        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: MyTeamExpenseRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Expense, mContext: Context) {
//            binding.tvTitle.text = item.title.toString()
//            when(expenseEvent){
//                Pending -> {
//                    binding.tvDelete.visibility = View.VISIBLE
//                }
//                Approved -> {
//                    binding.tvDelete.visibility = View.GONE
//                }
//            }
            binding.tvDate.text = item.name.valueQualifier()
            binding.status.text = item.grand_total.toString().valueQualifier()
            binding.tvHour.text = item.posting_date.valueQualifier()
//            binding.profileImg.setUrlImage(item.image, mContext)

        }
    }

}