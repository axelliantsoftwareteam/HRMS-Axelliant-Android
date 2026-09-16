package com.axelliant.hris.adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R

import com.axelliant.hris.databinding.UpcomingLeavesRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.leave.UpcomingLeaves

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

            binding.tvStartEnd.text = item.from_date.plus("-").plus(item.from_date).valueQualifier()
            binding.tvReason.text = item.reason.valueQualifier()
//            // Assuming `item.status` is of type `LeaveStatus`
            when (item.status) {
                LeaveStatus.DRAFT.value -> {
                    binding.tvStatus.apply {
                        text = item.status
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.pendingColor))
                        setTypeface(null, Typeface.NORMAL) // Set text style to normal
                    }

                }
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
