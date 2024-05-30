package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.BirthdayRowBinding
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.dashboard.Birthday

class BirthdayAdapter(
    private val context: Context,
    private val list: List<Birthday>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<BirthdayAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BirthdayRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], context)

        holder.binding.lyDay.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: BirthdayRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Birthday, context: Context) {

            binding.ivProfile.setUrlImage(item.image,context)
            binding.tvName.text = item.employee_name
            binding.tvDate.text = item.date_of_birth

        }
    }

}
