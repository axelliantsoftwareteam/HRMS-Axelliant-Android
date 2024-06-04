package com.axelliant.hrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.Test
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.databinding.MyAttendanceDetailRowBinding
import com.axelliant.hrms.databinding.SpinnerItemBinding
import com.axelliant.hrms.model.attendance.AttendanceDetail

class MyAttendanceDetailAdapter(
    private val attendanceList: ArrayList<AttendanceDetail>
) :
    RecyclerView.Adapter<MyAttendanceDetailAdapter.AccountsVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MyAttendanceDetailRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)

    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(attendanceList[position])

//        holder.binding.lyDropDown.isVisible = false


        holder.binding.tvDropDown.setOnClickListener {

//            holder.binding.lyDropDown.isVisible = !holder.binding.lyDropDown.isVisible

            attendanceList[position].isDetailVisible = !attendanceList[position].isDetailVisible
            notifyItemChanged(position)
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
                       binding.status.text = attendanceDetail.status
            binding.tvAttendStatus.text = attendanceDetail.requested
            binding.tvShiftTxt.text = attendanceDetail.shift
            binding.tvShiftTimeTxt.text = attendanceDetail.shift_timings
            binding.tvActualInTxt.text = attendanceDetail.in_time
            binding.tvExpectedInTxt.text = attendanceDetail.expected_in
            binding.tvActualOutTxt.text = attendanceDetail.out_time
            binding.tvExpectedOutTxt.text = attendanceDetail.expected_out

            binding.lyDropDown.isVisible = attendanceDetail.isDetailVisible

        }
    }

}