package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.CheckInListRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.extention.nullToEmpty
import com.axelliant.hris.model.checkin.CheckInDetail

class CheckInListAdapter(
    private val attendanceList: ArrayList<CheckInDetail>,
    private val mContext: Context,
    private val adapterItemClick: AdapterItemClick
) :
    RecyclerView.Adapter<CheckInListAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CheckInListRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(attendanceList[position])



        holder.binding.tvAttendStatus.setOnClickListener {
            adapterItemClick.onItemClick(attendanceList[position], position)
        }

        holder.binding.tvDropDown.setOnClickListener {

            attendanceList[position].isDetailVisible = !attendanceList[position].isDetailVisible
            notifyItemChanged(position)
        }
        when (attendanceList[position].requeststatus) {
            LeaveStatus.DRAFT.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(mContext, R.color.yellow))
            }
            LeaveStatus.PENDING.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_yellow)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(mContext, R.color.yellow))
            }
            LeaveStatus.APPROVED.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.light_green)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(mContext, R.color.green))
            }
            LeaveStatus.REJECTED.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(mContext, R.color.color_third_light)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(mContext, R.color.color_third))
            }
        }

    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    class AccountsVH(val binding: CheckInListRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(attendanceDetail: CheckInDetail) {

            binding.tvDate.text = attendanceDetail.time
            binding.tvHour.text = attendanceDetail.log_type
            binding.status.text = attendanceDetail.requeststatus
            binding.tvShiftTxt.text = attendanceDetail.location
            binding.tvShiftTimeTxt.text = attendanceDetail.reason.nullToEmpty()
            binding.lyDropDown.isVisible = attendanceDetail.isDetailVisible

        }
    }

}