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
import com.axelliant.hrms.extention.nullToEmpty
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.model.attendance.AttendanceApprovalObject
import com.axelliant.hrms.model.attendance.AttendanceData
import com.axelliant.hrms.utils.Utils.hideShow

class ApprovalsDetailAdapter(
    private val mContext: Context,
    private val detailArrayList: ArrayList<AttendanceApprovalObject>,
    private val approvedItemClick: AdapterItemClick,
    private val rejectItemClick: AdapterItemClick,
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

        holder.binding.tvApproved.setOnClickListener{
            approvedItemClick.onItemClick(detailArrayList[position],position)
        }

        holder.binding.tvReject.setOnClickListener{
            rejectItemClick.onItemClick(detailArrayList[position],position)

        }

    }

    override fun getItemCount(): Int {
        return detailArrayList.size
    }

    class AccountsVH(val binding: MyTeamApprovalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceApprovalObject, mContext: Context)
        {
            binding.tvEmployeName.text = item.employee_name
            binding.tvEmployeDesignation.text = item.designation
            binding.tvFromDateTxt.text = item.time.toString()
            binding.tvToDateTxt.text = item.location.toString()
            binding.tvLeaveTypeTxt.text = item.log_type
            binding.tvLeaveApproverTxt.text = item.requeststatus.toString()
            binding.tvReasonTxt.text = item.reason.nullToEmpty()



        }
    }

}