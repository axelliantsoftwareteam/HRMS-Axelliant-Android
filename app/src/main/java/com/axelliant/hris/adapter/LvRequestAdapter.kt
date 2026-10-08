package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.LvItemLeaveRequestBinding
import com.axelliant.hris.model.leaveatt.LeaveRecord
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.ui.WsTone

/** One card per leave request on the Leave screen. */
class LvRequestAdapter(private val onClick: (LeaveRecord) -> Unit) :
    ListAdapter<LeaveRecord, LvRequestAdapter.Holder>(DIFF) {

    class Holder(val binding: LvItemLeaveRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(LvItemLeaveRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            @Suppress("DEPRECATION")
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val leave = getItem(position)
        val ctx = holder.itemView.context
        val red = WsTone.leave(ctx)
        with(holder.binding) {
            WsStatus.iconBadge(lvLeaveIcon, red)
            lvLeaveType.text = leave.leave_type?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.lv_leave_fallback)
            lvLeaveDates.text = WsFormat.dateRange(leave.from_date, leave.to_date)
            WsStatus.badge(lvLeaveStatus, WsStatus.displayLeave(leave.rawStatus))
            lvLeaveDays.text = WsFormat.days(leave.days)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LeaveRecord>() {
            override fun areItemsTheSame(a: LeaveRecord, b: LeaveRecord) =
                (a.name ?: "${a.from_date}|${a.to_date}|${a.leave_type}") ==
                    (b.name ?: "${b.from_date}|${b.to_date}|${b.leave_type}")

            override fun areContentsTheSame(a: LeaveRecord, b: LeaveRecord) = a == b
        }
    }
}
