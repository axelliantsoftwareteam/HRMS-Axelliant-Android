package com.axelliant.hris.features.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.calendar.data.CalendarRepository
import com.axelliant.hris.features.calendar.domain.model.CalendarDayUiModel
import com.axelliant.hris.features.calendar.domain.model.CalendarEventUiModel
import com.axelliant.hris.features.calendar.domain.model.CalendarUiModel
import com.axelliant.hris.features.calendar.domain.model.CalendarViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<CalendarUiModel>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var selectedDate = Date()
    private var viewMode = CalendarViewMode.Month
    private var events = emptyList<CalendarEventUiModel>()
    private var lastUpdated = Date()
    private var searchQuery = ""

    fun loadCalendar() {
        viewModelScope.launch {
            publishState(isSyncing = true)
            val start = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                startOfDay()
            }
            val end = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.MONTH, 2)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                endOfDay()
            }

            when (val result = repository.getCalendarView(start.toGraphUtc(), end.toGraphUtc())) {
                is ApiResult.Success -> {
                    events = result.data.sortedBy { it.startDate ?: Date(0) }
                    lastUpdated = Date()
                    publishState()
                }
                is ApiResult.HttpError -> {
                    _uiState.value = UiState.Error(result.calendarErrorMessage())
                }
                is ApiResult.NetworkError -> _uiState.value = UiState.Error(result.message)
                is ApiResult.UnknownError -> _uiState.value = UiState.Error(result.message)
                ApiResult.Unauthorized -> {
                    _uiState.value = UiState.Error(
                        "Outlook calendar permission is missing. Please sign in with Microsoft again after calendar access is enabled."
                    )
                }
                ApiResult.Empty -> {
                    events = emptyList()
                    lastUpdated = Date()
                    publishState()
                }
            }
        }
    }

    fun selectMode(mode: CalendarViewMode) {
        if (viewMode == mode) return
        viewMode = mode
        publishState()
    }

    fun selectDate(date: Date) {
        selectedDate = date
        publishState()
    }

    fun goToToday() {
        selectedDate = Date()
        publishState()
    }

    fun goToPreviousMonth() {
        selectedDate = Calendar.getInstance().apply {
            time = selectedDate
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
        loadCalendar()
    }

    fun goToNextMonth() {
        selectedDate = Calendar.getInstance().apply {
            time = selectedDate
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
        loadCalendar()
    }

    fun updateSearch(query: String) {
        searchQuery = query
        publishState()
    }

    private fun publishState(isSyncing: Boolean = false) {
        _uiState.value = UiState.Success(
            CalendarUiModel(
                events = events,
                visibleEvents = filterEvents(),
                monthDays = buildMonthDays(),
                selectedDate = selectedDate,
                viewMode = viewMode,
                headerTitle = when (viewMode) {
                    CalendarViewMode.Month -> selectedDate.format("MMMM yyyy")
                    CalendarViewMode.Week -> weekTitle()
                    CalendarViewMode.Day -> selectedDate.format("EEEE, MMMM d, yyyy")
                },
                selectedDateTitle = selectedDate.format("EEEE, MMMM d, yyyy"),
                lastUpdatedText = "Updated ${lastUpdated.format("h:mm a")}",
                searchQuery = searchQuery,
                isSyncing = isSyncing
            )
        )
    }

    private fun filterEvents(): List<CalendarEventUiModel> {
        val dateFiltered = when (viewMode) {
            CalendarViewMode.Month -> events.filter { it.startDate.isSameDay(selectedDate) }
            CalendarViewMode.Week -> events.filter { it.startDate.isSameWeek(selectedDate) }
            CalendarViewMode.Day -> events.filter { it.startDate.isSameDay(selectedDate) }
        }
        val query = searchQuery.trim()
        if (query.isBlank()) return dateFiltered
        return dateFiltered.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
        }
    }

    private fun buildMonthDays(): List<CalendarDayUiModel> {
        val month = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val selectedMonth = month.get(Calendar.MONTH)
        val firstDayOffset = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        month.add(Calendar.DAY_OF_MONTH, -firstDayOffset)

        return List(42) {
            val date = month.time
            val hasEvents = events.any { it.startDate.isSameDay(date) }
            CalendarDayUiModel(
                date = date,
                dayNumber = month.get(Calendar.DAY_OF_MONTH).toString(),
                isInSelectedMonth = month.get(Calendar.MONTH) == selectedMonth,
                isSelected = date.isSameDay(selectedDate),
                hasEvents = hasEvents
            ).also {
                month.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    private fun weekTitle(): String {
        val start = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.time
        val end = Calendar.getInstance().apply {
            time = start
            add(Calendar.DAY_OF_MONTH, 6)
        }.time
        return "${start.format("MMM d")} - ${end.format("MMM d, yyyy")}"
    }

    private fun ApiResult.HttpError.calendarErrorMessage(): String {
        return when (code) {
            401, 403 -> "Outlook calendar permission is not available for this account. Calendar sync requires Microsoft Graph calendar access."
            else -> message.ifBlank { "Couldn't sync calendar. Please try again." }
        }
    }

    private fun Date?.isSameDay(other: Date): Boolean {
        if (this == null) return false
        val first = Calendar.getInstance().apply { time = this@isSameDay }
        val second = Calendar.getInstance().apply { time = other }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }

    private fun Date?.isSameWeek(other: Date): Boolean {
        if (this == null) return false
        val first = Calendar.getInstance().apply { time = this@isSameWeek }
        val second = Calendar.getInstance().apply { time = other }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.WEEK_OF_YEAR) == second.get(Calendar.WEEK_OF_YEAR)
    }

    private fun Date.format(pattern: String): String =
        SimpleDateFormat(pattern, Locale.US).format(this)

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.endOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    private fun Calendar.toGraphUtc(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(time)
    }
}
