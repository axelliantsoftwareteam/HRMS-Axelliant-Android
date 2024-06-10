package com.axelliant.hrms.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.Test
import com.axelliant.hrms.config.AppConst

import com.axelliant.hrms.databinding.RemaningLeaveRowBinding
import com.axelliant.hrms.databinding.UpcomingLeavesRowBinding
import com.axelliant.hrms.enums.LeaveStatus
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.leave.LeaveType
import com.axelliant.hrms.model.leave.UpcomingLeaves
import java.util.Date

class UpcomingLeaveAdapter(
    private val leaves: ArrayList<UpcomingLeaves>
) :
    RecyclerView.Adapter<UpcomingLeaveAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = UpcomingLeavesRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(leaves[position])
    }

    override fun getItemCount(): Int {
        return leaves.size
    }

    class AccountsVH(val binding: UpcomingLeavesRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UpcomingLeaves) {
            binding.tvTotalMemberTxt.text = item.leave_type.valueQualifier()
//
//            val date: Date? = item.from_date?.let { it1 -> AppConst.upComingLeavesinputFormat.parse(it1) }
//            val formattedTime: String =
//                date?.let { AppConst.upComingLeavesoutputFormat.format(it) } ?: "Invalid date"
//
//            val date1: Date? = item.from_date?.let { it1 -> AppConst.upComingLeavesinputFormat.parse(it1) }
//            val formattedEndTime: String =
//                date1?.let { AppConst.upComingLeavesoutputEndFormat.format(it) } ?: "Invalid date"


            binding.tvStartEnd.text = item.from_date.plus("-").plus(item.from_date).valueQualifier()
            binding.tvReason.text = item.reason.valueQualifier()
//            // Assuming `item.status` is of type `LeaveStatus`
            when (item.status) {
                LeaveStatus.PENDING.value -> {
                    binding.tvStatus.apply {
                        text = item.status
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.pendingColor))
                        setTypeface(null, Typeface.NORMAL) // Set text style to normal
                    }

                }
                LeaveStatus.APPROVED.value -> {
                    binding.tvStatus.apply {
                        text = item.status
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.approvedColor))
                        setTypeface(null, Typeface.BOLD) // Set text style to bold
                    }

                }
                LeaveStatus.REJECTED.value -> {
                    binding.tvStatus.apply {
                        text = item.status
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.rejectedColor))
                        setTypeface(null, Typeface.NORMAL) // Set text style to normal
                    }

                }
            }

            binding.tvDay.text = item.total_leave_days.toString().valueQualifier()
        }
    }

}