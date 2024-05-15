package com.axelliant.android_erp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.ExpenseRowBinding
import com.axelliant.android_erp.screens.ExpenseEvents
import com.axelliant.android_erp.screens.ExpenseEvents.*

class ExpenseAdapter(
    private val list: List<Test>,
    private val expenseEvent:ExpenseEvents,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<ExpenseAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ExpenseRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position],expenseEvent)

        holder.binding.tvViewDetail.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: ExpenseRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test,expenseEvent:ExpenseEvents) {
//            binding.tvTitle.text = item.title.toString()
            when(expenseEvent){
                Pending -> {
                    binding.tvDelete.visibility = View.VISIBLE
                }
                Approved -> {
                    binding.tvDelete.visibility = View.GONE
                }
            }


        }
    }

}