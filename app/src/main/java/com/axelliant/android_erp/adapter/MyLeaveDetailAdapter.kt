package com.axelliant.android_erp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.MyAttendanceDetailRowBinding
import com.axelliant.android_erp.databinding.MyLeaveDetailRowBinding

class MyLeaveDetailAdapter(
    private val list: List<Test>
) :
    RecyclerView.Adapter<MyLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyLeaveDetailRowBinding.inflate(layoutInflater, parent, false)
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

    class AccountsVH(val binding: MyLeaveDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test) {
//            binding.tvTitle.text = item.title.toString()

        }
    }

}