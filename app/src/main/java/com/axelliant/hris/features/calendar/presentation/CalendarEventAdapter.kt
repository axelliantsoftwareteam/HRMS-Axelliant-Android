package com.axelliant.hris.features.calendar.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.ItemCalendarEventBinding
import com.axelliant.hris.features.calendar.domain.model.CalendarEventUiModel

class CalendarEventAdapter(
    private val onEventClick: (CalendarEventUiModel) -> Unit
) : RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {
    private val items = mutableListOf<CalendarEventUiModel>()

    fun submitList(events: List<CalendarEventUiModel>) {
        items.clear()
        items.addAll(events)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(
            ItemCalendarEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EventViewHolder(
        private val binding: ItemCalendarEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CalendarEventUiModel) = with(binding) {
            eventAccent.backgroundTintList = ColorStateList.valueOf(item.accentColor)
            eventDay.text = item.dayLabel
            eventTime.text = if (item.isAllDay) "All day" else item.timeRange
            eventTitle.text = item.title
            eventSubtitle.text = item.subtitle
            teamsIcon.isVisible = item.isTeamsMeeting
            root.setOnClickListener { onEventClick(item) }
        }
    }
}
