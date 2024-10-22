package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.MyAttendanceDetailRowBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.LocationFilter
import com.axelliant.hris.model.attendance.AttendanceDetail

class MyAttendanceDetailAdapter(
    private val attendanceList: ArrayList<AttendanceDetail>,
    private val context: Context,
    private val adapterItemClick: AdapterItemClick
) :
    RecyclerView.Adapter<MyAttendanceDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyAttendanceDetailRowBinding.inflate(layoutInflater, parent, false)
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

        when (attendanceList[position].status) {
            LeaveStatus.Absent.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_red)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.color_third))
            }
            LeaveStatus.Present.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_green)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.green))
            }
            LeaveStatus.OnLeave.value -> {

                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.purple_bg)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.purple))
            }
        }

    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    class AccountsVH(val binding: MyAttendanceDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(attendanceDetail: AttendanceDetail) {
//            binding.tvTitle.text = item.title.toString()


            if (attendanceDetail.working_hours.toString() == "0")
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hr")
            else if (attendanceDetail.working_hours.toString() == "1")
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hr")
            else
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hrs")

            binding.tvDate.text = attendanceDetail.date

            binding.tvAttendStatus.text = attendanceDetail.custom_attendance_status


            binding.tvShiftTxt.text = attendanceDetail.shift
            binding.tvShiftTimeTxt.text = attendanceDetail.shift_timings
            binding.tvActualInTxt.text = attendanceDetail.in_time
            binding.tvExpectedInTxt.text = attendanceDetail.expected_in
            binding.tvActualOutTxt.text = attendanceDetail.out_time
            binding.tvExpectedOutTxt.text = attendanceDetail.expected_out

            binding.lyDropDown.isVisible = attendanceDetail.isDetailVisible


            binding.status.text = attendanceDetail.status


        }
    }

}