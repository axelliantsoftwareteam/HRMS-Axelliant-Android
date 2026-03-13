package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.BirthdayRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.dashboard.Birthday

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
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClick.onItemClick(list[adapterPosition], adapterPosition)
            }
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
