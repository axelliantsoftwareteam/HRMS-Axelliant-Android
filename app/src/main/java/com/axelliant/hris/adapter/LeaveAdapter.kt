package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.Test
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.LeaveRowBinding

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
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClick.onItemClick(list[adapterPosition], adapterPosition)
            }
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
