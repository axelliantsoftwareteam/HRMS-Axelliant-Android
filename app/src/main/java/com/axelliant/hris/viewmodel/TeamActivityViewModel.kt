package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.team.TeamArrival
import com.axelliant.hris.model.team.TeamAttendanceData
import com.axelliant.hris.model.team.TeamCheckinData
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.model.team.TeamMember
import com.axelliant.hris.model.team.TeamQuotaData
import com.axelliant.hris.model.team.TeamQuotaEmployee
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

/** The three lists Team Activity shows, one at a time. */
enum class TeamSegment { ARRIVALS, HOURS, QUOTA }

/** Date range plus client-side text filters of the Team Activity filter sheet. */
data class TeamActivityFilters(
    val startDate: String = TeamDates.firstOfMonth(),
    val endDate: String = TeamDates.today(),
    val employee: String = "",
    val designation: String = "",
    val location: String = "",
    val status: String = ""
) {
    companion object {
        fun default() = TeamActivityFilters()
    }
}

/**
 * Loads Team Activity for managers: working hours for the date range, today's arrivals in the office
 * and from home, and team leave balances. Text filters apply to the loaded data on the device.
 */
class TeamActivityViewModel(private val api: ApiInterface) : BaseViewModel() {

    var members: List<TeamMember> = emptyList()
        private set
    var arrivals: List<TeamArrival> = emptyList()
        private set
    var balances: List<TeamQuotaEmployee> = emptyList()
        private set
    var hasLoaded = false
        private set

    /** True when any of the four requests failed; the screen offers "Try again". */
    var loadFailed = false
        private set
    var filters = TeamActivityFilters.default()
        private set
    var segment = TeamSegment.ARRIVALS
    var quotaSearch = ""

    private val _changed = MutableLiveData<Unit>()

    /** Fires whenever loaded data changes so the screen re-renders its list. */
    val changed: LiveData<Unit> get() = _changed

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> get() = _error

    private var pending = 0
    private var officeArrivals: List<TeamArrival> = emptyList()
    private var homeArrivals: List<TeamArrival> = emptyList()

    /** Whether any filter differs from the default (dates this month so far, no text). */
    val hasActiveFilters: Boolean
        get() {
            val base = TeamActivityFilters.default()
            return filters.employee.isNotBlank() || filters.designation.isNotBlank() ||
                filters.location.isNotBlank() || filters.status.isNotBlank() ||
                filters.startDate != base.startDate || filters.endDate != base.endDate
        }

    fun displayName(member: TeamMember): String = member.name?.trim().orEmpty().ifEmpty { "Team member" }

    fun displayName(arrival: TeamArrival): String {
        val name = arrival.employee_name?.trim().orEmpty()
        if (name.isNotEmpty()) return name
        return members.firstOrNull { it.identity == arrival.employee }?.let { displayName(it) } ?: "Team member"
    }

    fun designation(arrival: TeamArrival): String? =
        members.firstOrNull { it.identity == arrival.employee }?.designation

    private fun matches(value: String?, query: String): Boolean =
        query.isBlank() || value.orEmpty().contains(query.trim(), ignoreCase = true)

    val filteredMembers: List<TeamMember>
        get() = members.filter { matches(displayName(it), filters.employee) && matches(it.designation, filters.designation) }

    val filteredArrivals: List<TeamArrival>
        get() = arrivals.filter { a ->
            matches(displayName(a), filters.employee) &&
                matches(designation(a), filters.designation) &&
                matches(a.location, filters.location) &&
                matches(a.status ?: a.location, filters.status)
        }

    /** Every member's balances matching the quota search; the signed-in manager is listed too. */
    val filteredQuota: List<TeamQuotaEmployee>
        get() {
            val query = quotaSearch.trim()
            return if (query.isEmpty()) balances
            else balances.filter { it.employee_name.orEmpty().contains(query, ignoreCase = true) }
        }

    /** The leave balance HRIS holds for a member, matched by employee id, else by name. */
    fun balanceFor(member: TeamMember): TeamQuotaEmployee? {
        val name = displayName(member)
        return balances.firstOrNull { it.employee.equals(member.identity, ignoreCase = true) }
            ?: balances.firstOrNull { it.employee_name.equals(name, ignoreCase = true) }
    }

    /** Loads all four sources together; [done] runs when every request has finished. */
    fun load(done: (() -> Unit)? = null) {
        if (pending > 0) {
            done?.invoke()
            return
        }
        if (!hasLoaded) isLoading.value = Event(true)
        loadFailed = false
        pending = 4
        val auth = AppConst.authHeader()
        val finish: () -> Unit = {
            pending -= 1
            if (pending == 0) {
                arrivals = (officeArrivals + homeArrivals)
                    .sortedByDescending { it.time_iso ?: it.date ?: "" }
                    .take(10)
                hasLoaded = true
                isLoading.value = Event(false)
                if (loadFailed) _error.value = Event("We could not load team activity right now.")
                _changed.value = Unit
                done?.invoke()
            }
        }
        val fail: () -> Unit = { loadFailed = true }

        api.getTeamAttendance(auth, filters.startDate, filters.endDate)
            .enqueueResult<TeamAttendanceData>(TeamAttendanceData::class.java) { result ->
                when (result) {
                    is ApiResult.Success -> members = result.data.attendance_data.orEmpty()
                    is ApiResult.Failure -> fail()
                }
                finish()
            }
        api.getTeamCheckins(auth, "In Office")
            .enqueueResult<TeamCheckinData>(TeamCheckinData::class.java) { result ->
                when (result) {
                    is ApiResult.Success -> officeArrivals = result.data.employee_list.orEmpty()
                    is ApiResult.Failure -> fail()
                }
                finish()
            }
        api.getTeamCheckins(auth, "Work From Home")
            .enqueueResult<TeamCheckinData>(TeamCheckinData::class.java) { result ->
                when (result) {
                    is ApiResult.Success -> homeArrivals = result.data.employee_list.orEmpty()
                    is ApiResult.Failure -> fail()
                }
                finish()
            }
        api.postTeamLeaveQuota(auth, emptyMap())
            .enqueueResult<TeamQuotaData>(TeamQuotaData::class.java) { result ->
                when (result) {
                    is ApiResult.Success -> {
                        balances = result.data.employees.orEmpty()
                        finish()
                    }
                    is ApiResult.Failure -> {
                        // The first Android version used GET; try it before giving up.
                        api.callTeamLeaveQuota(auth)
                            .enqueueResult<TeamQuotaData>(TeamQuotaData::class.java) { fallback ->
                                when (fallback) {
                                    is ApiResult.Success -> balances = fallback.data.employees.orEmpty()
                                    is ApiResult.Failure -> fail()
                                }
                                finish()
                            }
                    }
                }
            }
    }

    /**
     * Applies the filter sheet and reloads (the dates may have changed).
     * Returns false when the start date is after the end date.
     */
    fun applyFilters(newFilters: TeamActivityFilters): Boolean {
        if (newFilters.startDate > newFilters.endDate) return false
        filters = newFilters
        _changed.value = Unit
        load()
        return true
    }

    /** Re-renders after a purely client-side change (segment, search). */
    fun notifyChanged() {
        _changed.value = Unit
    }
}
