package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyTeamApprovalRowBinding
import com.axelliant.hris.extention.nullToEmpty
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.attendance.AttendanceApprovalObject
import com.axelliant.hris.utils.Utils.hideShow

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
        val item = detailArrayList[position]
        holder.bind(item, mContext)

        holder.binding.dropDown.setOnClickListener {
            holder.binding.lyAttendStatus.hideShow(it)
            holder.binding.divider.isVisible = holder.binding.lyAttendStatus.isVisible
        }

        holder.binding.tvApproved.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                approvedItemClick.onItemClick(detailArrayList[adapterPosition], adapterPosition)
            }
        }

        holder.binding.tvReject.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                rejectItemClick.onItemClick(detailArrayList[adapterPosition], adapterPosition)
            }
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
            binding.profileImg.setUrlImage(item.image, mContext)


        }
    }

}
