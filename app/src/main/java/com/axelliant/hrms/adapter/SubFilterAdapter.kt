package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.Test
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.SubFilterRowBinding
import com.axelliant.hrms.utils.Utils

class SubFilterAdapter(
    private val list: List<Test>,
    private val context: Context,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<SubFilterAdapter.AccountsVH>() {

    private var selectedPos = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubFilterRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    private fun setCurrentPosition(currentPos: Int = 0) {
        selectedPos = currentPos
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], position, context)
        holder.binding.lyWorkHome.setOnClickListener {
            setCurrentPosition(position)
            notifyDataSetChanged()
            itemClick.onItemClick(list[position], position)

        }

        if (position == selectedPos) {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.white))
            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.enable_rounded_bgg)
        } else {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.black))

            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.rounded_bgg)
        }

        holder.binding.tvWorkFrom.text = list[position].testString

        holder.binding.tvWorkTxt.text = Utils.getRandomString()


    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: SubFilterRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test, position: Int, context: Context) {


        }
    }

}