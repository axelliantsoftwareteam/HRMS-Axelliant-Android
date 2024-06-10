package com.axelliant.hrms.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.hrms.databinding.RemaningLeaveRowBinding
import com.axelliant.hrms.model.leave.LeaveType

class RemainingLeaveAdapter(
    private val leaves: ArrayList<LeaveType>
) :
    RecyclerView.Adapter<RemainingLeaveAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RemaningLeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position])
        if((position+1)%3==0)
        {
            holder.binding.lyCasual
        }
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: RemaningLeaveRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LeaveType) {
            binding.tvTitle.text = item.title
            binding.tvValue.text = item.value.toString()

        }
    }

}