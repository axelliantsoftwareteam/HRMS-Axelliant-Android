package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.SubFilterRowBinding
import com.axelliant.hris.model.dashboard.FilterModel

class SubFilterAdapter(
    private var filterId: String? = null,
    private val list: List<FilterModel>,
    private val context: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<SubFilterAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubFilterRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val item = list[position]
        holder.bind(item, position, context)
        holder.binding.lyWorkHome.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val currentItem = list[adapterPosition]
                filterId = currentItem.id.toString()
                notifyDataSetChanged()
                itemClick.onItemClick(currentItem, adapterPosition)
            }

        }

        holder.binding.tvWorkTxt.isVisible = item.id != ""

        if (filterId == item.id)
        {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.white))
            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.enable_rounded_bgg)
        } else {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.black))

            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.rounded_bgg)
        }

        holder.binding.tvWorkFrom.text = item.title
        holder.binding.tvWorkTxt.text = item.count


    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: SubFilterRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterModel, position: Int, context: Context) {}
    }

}
