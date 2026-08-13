package com.axelliant.hris.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.ModulesRowBinding
import com.axelliant.hris.model.Modules

class ModulesAdapter(
    private val list: List<Modules>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<ModulesAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ModulesRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position])

        holder.binding.lyModule.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClick.onItemClick(list[adapterPosition], adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: ModulesRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Modules) {
            val context = binding.root.context
            binding.tvModule.text = item.name
            binding.tvDescription.text = item.description
            binding.ivLoc.setImageDrawable(item.drawable)
            binding.tvModule.setTextColor(ContextCompat.getColor(context, R.color.ds_text_primary))
            binding.tvDescription.setTextColor(ContextCompat.getColor(context, R.color.ds_text_secondary))
            binding.ivLoc.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.ds_primary)
            )
        }
    }

}
