package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamAttendRowBinding
import com.axelliant.hris.databinding.TodayMyTeamAttendRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.todayTeam.EmployTeamProfile

class TodayTeamAttendanceDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<EmployTeamProfile>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<TodayTeamAttendanceDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TodayMyTeamAttendRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(detailArrayList[position], mContext)

    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: TodayMyTeamAttendRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmployTeamProfile, mContext: Context) {
            binding.tvEmployeName.text = item.employee_name.toString()
            binding.tvEmployeDesignation.text = item.date.toString()
            binding.profileImg.setUrlImage(item.image, mContext)


        }
    }

}