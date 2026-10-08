package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.team.CalendarStatus
import com.axelliant.hris.model.team.TeamCalendarData
import com.axelliant.hris.model.team.TeamCalendarMember
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Loads and filters the team calendar: the current month by default, with start/end date reloads and
 * client-side employee and designation filters (HRIS returns the whole team whatever they are).
 */
class TeamCalendarViewModel(private val api: ApiInterface) : BaseViewModel() {

    var startDate: String = TeamDates.firstOfMonth()
        private set
    var endDate: String = TeamDates.lastOfMonth()
        private set
    var dates: List<String> = emptyList()
        private set
    var members: List<TeamCalendarMember> = emptyList()
        private set
    var hasLoaded = false
        private set
    var appliedEmployee = ""
        private set
    var appliedDesignation = ""
        private set

    /** Statuses picked from the legend; empty shows every day normally. */
    var highlighted: Set<CalendarStatus> = emptySet()
        private set

    private val _changed = MutableLiveData<Unit>()
    val changed: LiveData<Unit> get() = _changed

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> get() = _error

    private var requestId = 0

    val visibleMembers: List<TeamCalendarMember>
        get() = members.filter { m ->
            (appliedEmployee.isEmpty() || m.employee_name.orEmpty().contains(appliedEmployee, ignoreCase = true)) &&
                (appliedDesignation.isEmpty() || m.designation.orEmpty().contains(appliedDesignation, ignoreCase = true))
        }

    /** "September 2026" for a full month, else "Sep 1 – Oct 15, 2026". */
    val rangeTitle: String
        get() {
            val start = TeamDates.calendar(startDate)
            val end = TeamDates.calendar(endDate)
            val fullMonth = TeamDates.firstOfMonth(start) == startDate && TeamDates.lastOfMonth(start) == endDate
            return if (fullMonth) {
                SimpleDateFormat("MMMM yyyy", Locale.US).format(start.time)
            } else {
                val first = SimpleDateFormat("MMM d", Locale.US).format(start.time)
                val last = SimpleDateFormat("MMM d, yyyy", Locale.US).format(end.time)
                "$first – $last"
            }
        }

    /** True while a filter other than the default month is applied. */
    val hasActiveFilters: Boolean
        get() = appliedEmployee.isNotEmpty() || appliedDesignation.isNotEmpty() || highlighted.isNotEmpty() ||
            startDate != TeamDates.firstOfMonth() || endDate != TeamDates.lastOfMonth()

    /** How many days a member had in each status (statuses without days are left out). */
    fun counts(member: TeamCalendarMember): List<Pair<CalendarStatus, Int>> {
        val map = HashMap<CalendarStatus, Int>()
        member.days.orEmpty().forEach { d -> CalendarStatus.of(d).let { map[it] = (map[it] ?: 0) + 1 } }
        return CalendarStatus.values().filter { (map[it] ?: 0) > 0 }.map { it to map.getValue(it) }
    }

    /** Moves the view one month back or forward and shows that whole month. */
    fun shiftMonth(by: Int) {
        val c = TeamDates.calendar(startDate)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, by)
        setRange(TeamDates.firstOfMonth(c), TeamDates.lastOfMonth(c))
    }

    /** Applies the filter sheet. Returns false when the end date is before the start date. */
    fun applyFilters(start: String, end: String, employee: String, designation: String): Boolean {
        if (end < start) return false
        appliedEmployee = employee.trim()
        appliedDesignation = designation.trim()
        setRange(start, end)
        return true
    }

    /** Clears every filter and returns to the current month. */
    fun resetFilters() {
        appliedEmployee = ""
        appliedDesignation = ""
        highlighted = emptySet()
        setRange(TeamDates.firstOfMonth(), TeamDates.lastOfMonth())
    }

    /** Picks or clears a legend status. */
    fun toggleHighlight(status: CalendarStatus) {
        highlighted = if (status in highlighted) highlighted - status else highlighted + status
        _changed.value = Unit
    }

    private fun setRange(start: String, end: String) {
        startDate = start
        endDate = end
        _changed.value = Unit
        load()
    }

    /** Loads the calendar for the current range; [done] runs when the request ends. */
    fun load(done: (() -> Unit)? = null) {
        val id = ++requestId
        if (members.isEmpty()) isLoading.value = Event(true)
        api.getTeamCalendar(AppConst.authHeader(), startDate, endDate)
            .enqueueResult<TeamCalendarData>(TeamCalendarData::class.java) { result ->
                if (id != requestId) {
                    done?.invoke()
                    return@enqueueResult
                }
                isLoading.value = Event(false)
                hasLoaded = true
                when (result) {
                    is ApiResult.Success -> {
                        dates = result.data.dates.orEmpty()
                        members = result.data.employees.orEmpty()
                    }
                    is ApiResult.Failure -> _error.value = Event("We could not load the team calendar right now.")
                }
                _changed.value = Unit
                done?.invoke()
            }
    }
}
