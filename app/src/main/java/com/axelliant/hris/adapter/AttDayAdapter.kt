package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.AttItemDayBinding
import com.axelliant.hris.model.leaveatt.AttendanceDay
import com.axelliant.hris.ui.WsStatus

/** One card per attendance day: status edge, date, time window, location, status badge and hours. */
class AttDayAdapter(private val onClick: (AttendanceDay) -> Unit) :
    ListAdapter<AttendanceDay, AttDayAdapter.Holder>(DIFF) {

    class Holder(val binding: AttItemDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(AttItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        // Cards are separated by a gap, not by margins, so the left edge stays flush.
        (holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin =
            parent.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
        holder.itemView.setOnClickListener {
            @Suppress("DEPRECATION")
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) onClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val day = getItem(position)
        val ctx = holder.itemView.context
        val statusColor = AttFormat.statusColor(ctx, day.shownStatus)
        with(holder.binding) {
            attDayEdge.setBackgroundColor(statusColor)
            attDayDate.text = AttFormat.dayHeading(day.date)
            attDayTime.text = AttFormat.timeWindow(ctx, day)
            val location = day.custom_attendance_status.orEmpty()
            attDayLocationRow.visibility = if (location.isBlank()) View.GONE else View.VISIBLE
            attDayLocation.text = location
            WsStatus.badge(attDayStatus, day.shownStatus, statusColor)
            attDayHours.text = AttFormat.hours(ctx, day)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AttendanceDay>() {
            override fun areItemsTheSame(a: AttendanceDay, b: AttendanceDay) = a.date == b.date && a.shift == b.shift
            override fun areContentsTheSame(a: AttendanceDay, b: AttendanceDay) = a == b
        }
    }
}
