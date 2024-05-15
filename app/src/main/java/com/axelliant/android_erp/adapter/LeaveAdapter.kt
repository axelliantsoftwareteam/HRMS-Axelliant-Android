package com.axelliant.android_erp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.LeaveRowBinding
import com.axelliant.android_erp.databinding.WeeklyRowBinding

class LeaveAdapter(
    private val list: List<Test>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<LeaveAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position])

        holder.binding.lyWeekly.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: LeaveRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test) {
//            binding.tvTitle.text = item.title.toString()

        }
    }

}