package com.axelliant.hrms.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.Test
import com.axelliant.hrms.databinding.MyTeamLeaveRowBinding

class TeamLeaveDetailAdapter(
    private val list: List<Test>
) :
    RecyclerView.Adapter<TeamLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamLeaveRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position])
        holder.binding.lyDropDown.isVisible = false

        holder.binding.tvDropDown.setOnClickListener {

            holder.binding.lyDropDown.isVisible = !holder.binding.lyDropDown.isVisible
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: MyTeamLeaveRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test) {
//            binding.tvTitle.text = item.title.toString()

        }
    }

}