package com.axelliant.android_erp.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.MyAttendanceDetailRowBinding
import com.axelliant.android_erp.databinding.MyTeamAttendRowBinding
import com.axelliant.android_erp.databinding.SubFilterRowBinding
import com.axelliant.android_erp.model.dashboard.AttendanceStatus
import com.axelliant.android_erp.utils.Utils

class SubFilterAdapter(
    private var filterId: String = "",
    private val list: List<AttendanceStatus>,
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
        holder.bind(list[position], position, context)
        holder.binding.lyWorkHome.setOnClickListener {
            filterId = list[position].id.toString()
            notifyDataSetChanged()
            itemClick.onItemClick(list[position], position)

        }

        if (filterId == list[position].id) {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.white))
            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.enable_rounded_bgg)
        } else {
            holder.binding.tvWorkFrom.setTextColor(context.getColor(R.color.black))

            holder.binding.lyWorkHome.background =
                context.resources.getDrawable(R.drawable.rounded_bgg)
        }

        holder.binding.tvWorkFrom.text = list[position].title
        holder.binding.tvWorkTxt.text = list[position].count


    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: SubFilterRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceStatus, position: Int, context: Context) {


        }
    }

}