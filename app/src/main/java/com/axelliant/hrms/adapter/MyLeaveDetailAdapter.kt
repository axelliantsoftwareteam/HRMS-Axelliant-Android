package com.axelliant.hrms.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.Test
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyAttendanceDetailRowBinding
import com.axelliant.hrms.databinding.MyLeaveDetailRowBinding
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.leave.LeaveDetail

class MyLeaveDetailAdapter(
    private val leaves: ArrayList<LeaveDetail>,
    private val mContext: Context
) :
    RecyclerView.Adapter<MyLeaveDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyLeaveDetailRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position], mContext)

//        holder.binding.lyDropDown.isVisible = false

        holder.binding.tvDropDown.setOnClickListener {

//            holder.binding.lyDropDown.isVisible = !holder.binding.lyDropDown.isVisible

            leaves[position].isDetailVisible = !leaves[position].isDetailVisible
            notifyItemChanged(position)
        }


    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: MyLeaveDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(leaveDetail: LeaveDetail, mContext: Context) {
            binding.fromDate.text = leaveDetail.from_date
            binding.toDate.text = leaveDetail.to_date
            binding.tvLeaveDays.text = leaveDetail.total_leave_days.toString()
            if (leaveDetail.is_paid) {
                binding.tvPaid.text = "Paid"
                binding.tvPaid.setTextColor(mContext.getColor(R.color.black))

            } else {
                binding.tvPaid.text = "Un Paid"
                binding.tvPaid.setTextColor(mContext.getColor(R.color.red))

            }
            binding.tvType.text = leaveDetail.leave_type

            binding.tvReason.text = leaveDetail.leave_reason.valueQualifier()
            binding.tvAttendStatus.text = leaveDetail.status

            binding.lyDropDown.isVisible = leaveDetail.isDetailVisible

        }
    }

}