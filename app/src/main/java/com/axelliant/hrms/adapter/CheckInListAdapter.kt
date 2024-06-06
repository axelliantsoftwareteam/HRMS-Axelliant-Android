package com.axelliant.hrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.Test
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.CheckInListRowBinding
import com.axelliant.hrms.databinding.MyAttendanceDetailRowBinding
import com.axelliant.hrms.databinding.SpinnerItemBinding
import com.axelliant.hrms.extention.nullToEmpty
import com.axelliant.hrms.model.attendance.AttendanceDetail
import com.axelliant.hrms.model.checkin.CheckInDetail

class CheckInListAdapter(
    private val attendanceList: ArrayList<CheckInDetail>,
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


        holder.binding.lyActionBtn.setOnClickListener {
            adapterItemClick.onItemClick(attendanceList[position], position)
        }

        holder.binding.tvDropDown.setOnClickListener {

            attendanceList[position].isDetailVisible = !attendanceList[position].isDetailVisible
            notifyItemChanged(position)
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