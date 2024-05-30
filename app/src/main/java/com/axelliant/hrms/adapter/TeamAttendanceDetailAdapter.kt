package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyTeamAttendRowBinding
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.attendance.AttendanceData
import com.axelliant.hrms.utils.Utils.hideShow

class TeamAttendanceDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<AttendanceData>,
    private val itemClick: AdapterItemClick
) :
    RecyclerView.Adapter<TeamAttendanceDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamAttendRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(detailArrayList[position], mContext)

        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 3)
        holder.binding.rvLeaveCount.adapter =
            ValuesAdapter(detailArrayList[position].values!!, mContext)
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false


        holder.binding.lyWeekly.setOnClickListener {


            itemClick.onItemClick(detailArrayList[position], position)
        }

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.lineDiv.isVisible = holder.binding.rvLeaveCount.isVisible

        }


    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamAttendRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceData, mContext: Context) {
            binding.tvEmployeName.text = item.name.toString()
            binding.tvEmployeDesignation.text = item.designation.toString()
            binding.profileImg.setUrlImage(item.image, mContext)


        }
    }

}