package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamAttendRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.attendance.AttendanceData
import com.axelliant.hris.utils.Utils.hideShow

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
        val item = detailArrayList[position]
        holder.bind(item, mContext)

        holder.binding.rvLeaveCount.layoutManager = GridLayoutManager(mContext, 3)
        holder.binding.rvLeaveCount.adapter =
            ValuesAdapter(item.values!!, mContext)
        holder.binding.rvLeaveCount.isNestedScrollingEnabled = false


        holder.binding.lyWeekly.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                itemClick.onItemClick(detailArrayList[adapterPosition], adapterPosition)
            }
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
