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
import com.axelliant.hris.model.attendance.AttendanceDetail
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
        val item = attendanceList[position]
        holder.bind(item)


        holder.binding.lyActionBtn.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                adapterItemClick.onItemClick(attendanceList[adapterPosition], adapterPosition)
            }
        }

        holder.binding.tvDropDown.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val currentItem = attendanceList[adapterPosition]
                currentItem.isDetailVisible = !currentItem.isDetailVisible
                notifyItemChanged(adapterPosition)
            }
        }

        val statusValue = item.display_status.ifBlank { item.status }
        when {
            statusValue == LeaveStatus.Absent.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_red)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.color_third))
            }
            statusValue == LeaveStatus.Present.value -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_green)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.green))
            }
            statusValue == LeaveStatus.OnLeave.value -> {

                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.purple_bg)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.purple))
            }
            statusValue.contains("Weekend", ignoreCase = true) -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.purple_bg)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.purple))
            }
            statusValue.contains("Holiday", ignoreCase = true) -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_green)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.green))
            }
            else -> {
                holder.binding.status.backgroundTintList = ContextCompat.getColorStateList(context, R.color.light_green)
                holder.binding.status.setTextColor(ContextCompat.getColorStateList(context, R.color.green))
            }
        }

    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    class AccountsVH(val binding: MyAttendanceDetailRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(attendanceDetail: AttendanceDetail) {
            if (attendanceDetail.working_hours.toString() == "0")
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hr")
            else if (attendanceDetail.working_hours.toString() == "1")
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hr")
            else
                binding.tvHour.text = attendanceDetail.working_hours.toString().plus(" Hrs")

            binding.tvDate.text = attendanceDetail.date

            binding.tvAttendStatus.text = attendanceDetail.requested


            binding.tvShiftTxt.text = attendanceDetail.shift
            binding.tvShiftTimeTxt.text = attendanceDetail.shift_timings
            binding.tvActualInTxt.text = formatDisplayTime(attendanceDetail.in_time_iso, attendanceDetail.in_time)
            binding.tvExpectedInTxt.text = formatDisplayTime(attendanceDetail.expected_in_iso, attendanceDetail.expected_in)
            binding.tvActualOutTxt.text = formatDisplayTime(attendanceDetail.out_time_iso, attendanceDetail.out_time)
            binding.tvExpectedOutTxt.text = formatDisplayTime(attendanceDetail.expected_out_iso, attendanceDetail.expected_out)

            binding.lyDropDown.isVisible = attendanceDetail.isDetailVisible

            binding.status.text = attendanceDetail.display_status.ifBlank { attendanceDetail.status }


        }

        private fun formatDisplayTime(isoValue: String, fallback: String): String {
            if (isoValue.isNotBlank()) {
                parseIsoDate(isoValue)?.let { date ->
                    val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    displayFormat.timeZone = TimeZone.getDefault()
                    return displayFormat.format(date)
                }
            }
            return when (fallback) {
                "Weekend", "Holiday", "Check in Missing", "Check out Missing" -> "--"
                else -> fallback
            }
        }

        private fun parseIsoDate(value: String): java.util.Date? {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"
            )
            for (pattern in formats) {
                runCatching {
                    val formatter = SimpleDateFormat(pattern, Locale.US)
                    return formatter.parse(value)
                }
            }
            return null
        }
    }

}
