package com.axelliant.android_erp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.BirthdayRowBinding
import com.axelliant.android_erp.model.Birthday

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
        holder.bind(list[position],context)

        holder.binding.lyDay.setOnClickListener {
            itemClick.onItemClick(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: BirthdayRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Birthday, context: Context) {
//            binding.tvTitle.text = item.title.toString()
            binding.ivProfile.setImageDrawable(item.icon)
            binding.tvName.text = item.name
            binding.tvDate.text = item.dob
            if (item.dob == "Today")
                binding.ivCake.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_celebrations
                    )
                )
            else
                binding.ivCake.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_cake_tone
                    )
                )


        }
    }

}