package com.axelliant.android_erp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.Test

import com.axelliant.android_erp.databinding.RemaningLeaveRowBinding
import com.axelliant.android_erp.databinding.UpcomingLeavesRowBinding
import com.axelliant.android_erp.model.leave.LeaveType

class UpcomingLeaveAdapter(
    private val leaves: ArrayList<Test>
) :
    RecyclerView.Adapter<UpcomingLeaveAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = UpcomingLeavesRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position])
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: UpcomingLeavesRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test) {
/*            binding.tvTitle.text = item.title
            binding.tvValue.text = item.value.toString()*/
        }
    }

}