package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.team.TeamArrival
import com.axelliant.hris.model.team.TeamAttendanceData
import com.axelliant.hris.model.team.TeamCheckinData
import com.axelliant.hris.model.team.TeamDates
import com.axelliant.hris.model.team.TeamMember
import com.axelliant.hris.model.team.TeamQuotaData
import com.axelliant.hris.model.team.TeamQuotaEmployee
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult

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
open class TeamActivityViewModel(private val api: ApiInterface, private val batchFailure: (Boolean) -> String) : BaseViewModel() {

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

    /** Replaces obsolete batches; four sources (including the GET quota fallback) commit atomically. */
    fun load(kind: ListLoadKind = ListLoadKind.REFRESH) {
        val ticket = beginListLoad(kind = kind, replace = true)
        val selected = filters
        var remaining = 4
        var failed = false
        var newMembers: List<TeamMember> = emptyList()
        var office: List<TeamArrival> = emptyList()
        var home: List<TeamArrival> = emptyList()
        var quota: List<TeamQuotaEmployee> = emptyList()
        fun finish() {
            if (!isCurrentListLoad("list", ticket) || --remaining > 0) return
            loadFailed = failed
            if (!failed) {
                members = newMembers
                arrivals = (office + home).sortedByDescending { it.time_iso ?: it.date ?: "" }.take(10)
                balances = quota
                hasLoaded = true
                displayedFilters = selected
                finishListLoad("list", ticket, members.size + arrivals.size + balances.size)
            } else finishListLoad("list", ticket, error = batchFailure(hasLoaded))
            _changed.value = Unit
        }
        val auth = AppConst.authHeader()
        requestListPart<TeamAttendanceData>(api.getTeamAttendance(auth, selected.startDate, selected.endDate),
            TeamAttendanceData::class.java, "list", ticket) {
            if (it is ApiResult.Success) newMembers = it.data.attendance_data.orEmpty() else failed = true
            finish()
        }
        requestListPart<TeamCheckinData>(api.getTeamCheckins(auth, "In Office"), TeamCheckinData::class.java, "list", ticket) {
            if (it is ApiResult.Success) office = it.data.employee_list.orEmpty() else failed = true
            finish()
        }
        requestListPart<TeamCheckinData>(api.getTeamCheckins(auth, "Work From Home"), TeamCheckinData::class.java, "list", ticket) {
            if (it is ApiResult.Success) home = it.data.employee_list.orEmpty() else failed = true
            finish()
        }
        requestListPart<TeamQuotaData>(api.postTeamLeaveQuota(auth, emptyMap()), TeamQuotaData::class.java, "list", ticket) {
            if (it is ApiResult.Success) { quota = it.data.employees.orEmpty(); finish() }
            else requestListPart<TeamQuotaData>(api.callTeamLeaveQuota(auth), TeamQuotaData::class.java, "list", ticket) { fallback ->
                if (fallback is ApiResult.Success) quota = fallback.data.employees.orEmpty() else failed = true
                finish()
            }
        }
    }

    private var displayedFilters: TeamActivityFilters? = null
    val showingPreviousRange: Boolean get() = displayedFilters?.let {
        it.startDate != filters.startDate || it.endDate != filters.endDate
    } == true
    /**
     * Applies the filter sheet and reloads (the dates may have changed).
     * Returns false when the start date is after the end date.
     */
    fun applyFilters(newFilters: TeamActivityFilters): Boolean {
        if (newFilters.startDate > newFilters.endDate) return false
        filters = newFilters
        _changed.value = Unit
        load(ListLoadKind.FILTER)
        return true
    }

    /** Re-renders after a purely client-side change (segment, search). */
    fun notifyChanged() {
        _changed.value = Unit
    }
}
