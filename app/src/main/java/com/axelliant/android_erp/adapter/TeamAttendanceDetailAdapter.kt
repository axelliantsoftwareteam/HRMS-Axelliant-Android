package com.axelliant.android_erp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.MyAttendanceDetailRowBinding
import com.axelliant.android_erp.databinding.MyTeamAttendRowBinding
import com.axelliant.android_erp.model.dashboard.EmployProfile

class TeamAttendanceDetailAdapter(
    private val list: ArrayList<EmployProfile>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<TeamAttendanceDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamAttendRowBinding.inflate(layoutInflater, parent, false)
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

    class AccountsVH(val binding: MyTeamAttendRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmployProfile) {
            binding.tvName.text = item.name.toString()

        }
    }

}