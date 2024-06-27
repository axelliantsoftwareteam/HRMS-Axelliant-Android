package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.databinding.MyTeamExpenseSubRowBinding
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.expense.AddExpense

class ExpenseRowAdapter(
    private val list: List<AddExpense>,
    private val mContext: Context
) :
    RecyclerView.Adapter<ExpenseRowAdapter.AccountsVH>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamExpenseSubRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }


    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], position, mContext)


    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: MyTeamExpenseSubRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AddExpense, position: Int, mContext: Context) {
            binding.tvNo.text=((position+1).toString().plus("_"))
            binding.tvReasonTxt.text = item.description.toString().valueQualifier()
            binding.tvFromDateTxt.text = item.expense_type.toString()
            binding.tvAmountTxt.text = item.amount.toString()
            binding.tvToDateTxt.text = item.expense_date.toString()

        }
    }

}