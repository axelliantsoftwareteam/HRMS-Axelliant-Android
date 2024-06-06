package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.contentValuesOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyTeamApprovalRowBinding
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.attendance.AttendanceData
import com.axelliant.hrms.utils.Utils.hideShow

class ApprovalsDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<AttendanceData>,
    private val itemClick: AdapterItemClick,
    private val requestType: String,
    private val isRequestType: Boolean
) :
    RecyclerView.Adapter<ApprovalsDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyTeamApprovalRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(detailArrayList[position], mContext)

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.divider.isVisible = holder.binding.lyAttendStatus.isVisible
        }

        if (requestType == RequestFilter.ATTENDANCE.name)
        {
            holder.binding.tvFromDate.text=ContextCompat.getString(mContext,R.string.date)
            holder.binding.tvToDate.text=ContextCompat.getString(mContext,R.string.time)
            holder.binding.tvLeaveApprover.text=ContextCompat.getString(mContext,R.string.shift)
        }
        if (isRequestType)
        {
            holder.binding.tvApproved.visibility=View.VISIBLE
            holder.binding.tvReject.visibility=View.VISIBLE
        }
        else{
            holder.binding.tvApproved.visibility=View.GONE
            holder.binding.tvReject.visibility=View.GONE
        }
    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamApprovalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceData, mContext: Context)
        {

            binding.tvEmployeName.text = item.name.toString()
            binding.tvEmployeDesignation.text = item.designation.toString()
            binding.tvEmployeDesignation.text = item.designation.toString()
            binding.tvEmployeDesignation.text = item.designation.toString()
            binding.tvEmployeDesignation.text = item.designation.toString()
            binding.profileImg.setUrlImage(item.image, mContext)


        }
    }

}