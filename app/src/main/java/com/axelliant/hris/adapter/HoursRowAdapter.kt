package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.MyResourceHoursSubRowBinding
import com.axelliant.hris.databinding.MyTeamExpenseSubRowBinding
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.resourceManage.ProjectHour

class HoursRowAdapter(
    private val list: List<ProjectHour>,
    private val mContext: Context
) :
    RecyclerView.Adapter<HoursRowAdapter.AccountsVH>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyResourceHoursSubRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }


    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], position, mContext)

        if (list.size>1)
        {
         holder.binding.btnDivider.isVisible=true
        }
        else{
            holder.binding.btnDivider.isVisible=false
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: MyResourceHoursSubRowBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProjectHour, position: Int, mContext: Context) {

            binding.tvNo.text=((position+1).toString().plus("-"))
            binding.tvFromDateTxt.text = item.project_name.toString()
            binding.tvAmountTxt.text = item.working_hours.toString()
            binding.tvToDateTxt.text = item.date.toString()


        }
    }

}