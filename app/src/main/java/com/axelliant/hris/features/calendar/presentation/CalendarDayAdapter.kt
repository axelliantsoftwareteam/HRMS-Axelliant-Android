package com.axelliant.hris.features.calendar.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ItemCalendarDayBinding
import com.axelliant.hris.features.calendar.domain.model.CalendarDayUiModel

class CalendarDayAdapter(
    private val onDateClick: (CalendarDayUiModel) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {
    private val items = mutableListOf<CalendarDayUiModel>()

    fun submitList(days: List<CalendarDayUiModel>) {
        items.clear()
        items.addAll(days)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(
            ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CalendarDayUiModel) = with(binding) {
            dayText.text = item.dayNumber
            dayText.setTextColor(
                ContextCompat.getColor(
                    root.context,
                    when {
                        item.isSelected -> R.color.ds_neutral_white
                        item.isInSelectedMonth -> R.color.ds_text_primary
                        else -> R.color.ds_text_muted
                    }
                )
            )
            dayText.setBackgroundResource(
                if (item.isSelected) R.drawable.bg_calendar_day_selected else android.R.color.transparent
            )
            eventDot.isVisible = item.hasEvents
            root.setOnClickListener { onDateClick(item) }
        }
    }
}
