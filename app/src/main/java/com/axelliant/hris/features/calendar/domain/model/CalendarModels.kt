package com.axelliant.hris.features.calendar.domain.model

import java.util.Date

data class CalendarEventUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val dayLabel: String,
    val timeRange: String,
    val startDate: Date?,
    val endDate: Date?,
    val isAllDay: Boolean,
    val isTeamsMeeting: Boolean,
    val location: String,
    val organizerName: String,
    val organizerEmail: String,
    val attendeesSummary: String,
    val description: String,
    val webLink: String?,
    val onlineMeetingUrl: String?,
    val accentColor: Int
)

enum class CalendarViewMode {
    Month,
    Week,
    Day
}

data class CalendarDayUiModel(
    val date: Date,
    val dayNumber: String,
    val isInSelectedMonth: Boolean,
    val isSelected: Boolean,
    val hasEvents: Boolean
)

data class CalendarUiModel(
    val events: List<CalendarEventUiModel>,
    val visibleEvents: List<CalendarEventUiModel>,
    val monthDays: List<CalendarDayUiModel>,
    val selectedDate: Date,
    val viewMode: CalendarViewMode,
    val headerTitle: String,
    val selectedDateTitle: String,
    val lastUpdatedText: String,
    val searchQuery: String = "",
    val isSyncing: Boolean = false
)
