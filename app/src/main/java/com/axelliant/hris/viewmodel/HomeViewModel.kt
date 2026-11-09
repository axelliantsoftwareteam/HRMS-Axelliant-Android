package com.axelliant.hris.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.login.CheckInResponse
import com.axelliant.hris.model.todayTeam.EmployProfileListModel
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.repos.HomeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Retains Home content and refreshes independently, without permanent LiveData observers. */
class HomeViewModel(private val homeRepo: HomeSource) : BaseViewModel() {
    val dashboardResponse = MutableLiveData<DashboardResponse?>()
    val todayTeamResponse = MutableLiveData<TodayTeamResponse?>()
    val employListResponse = MutableLiveData<Event<EmployProfileListModel?>>()
    val checkInResponse = MutableLiveData<Event<CheckInResponse?>>()
    val homeError = MutableLiveData<Event<String>>()
    val refreshing = MutableLiveData(false)
    private var dashboardJob: Job? = null
    private var teamJob: Job? = null
    private var punchJob: Job? = null
    private var employeeJob: Job? = null
    private var employeeFilter: String? = null
    private var blockingRequests = 0

    private fun blocking(start: Boolean) {
        blockingRequests += if (start) 1 else -1
        isLoading.value = Event(blockingRequests > 0)
    }

    fun getDashboardInformation(force: Boolean = false) {
        if (dashboardJob?.isActive == true) return
        dashboardResponse.value = homeRepo.cachedDashboard()
        if (!force && homeRepo.dashboardIsFresh()) return
        dashboardJob = viewModelScope.launch {
            refreshing.value = true
            try {
                when (val result = homeRepo.dashboard(force)) {
                    is ApiResult.Success -> dashboardResponse.value = result.data
                    is ApiResult.Failure -> homeError.value = Event(result.message)
                }
            } finally { refreshing.value = false }
        }
    }

    fun getTodayTeamInfo() {
        if (teamJob?.isActive == true) return
        todayTeamResponse.value = homeRepo.cachedTeam()
        if (homeRepo.teamIsFresh()) return
        teamJob = viewModelScope.launch {
            when (val result = homeRepo.team()) {
                is ApiResult.Success -> todayTeamResponse.value = result.data
                is ApiResult.Failure -> homeError.value = Event(result.message)
            }
        }
    }

    /** Never queues/replays punches; expired attendance must be refreshed and reviewed first. */
    fun postCheckIn(body: CheckInRequest) {
        if (punchJob?.isActive == true) return
        if (dashboardJob?.isActive == true) {
            homeError.value = Event("Refreshing attendance. Please wait and try again.")
            return
        }
        if (!homeRepo.dashboardIsFresh()) {
            homeError.value = Event("Refreshing attendance. Review the updated action and try again.")
            getDashboardInformation(force = true)
            return
        }
        punchJob = viewModelScope.launch {
            blocking(true)
            try {
                when (val result = homeRepo.punch(body)) {
                    is ApiResult.Success -> {
                        checkInResponse.value = Event(result.data)
                        dashboardJob?.cancel()
                        teamJob?.cancel()
                        // Cancelled jobs cannot write back into the invalidated caches.
                        dashboardJob?.join()
                        teamJob?.join()
                        getDashboardInformation(force = true)
                    }
                    is ApiResult.Failure -> homeError.value = Event(result.message)
                }
            } finally { blocking(false) }
        }
    }

    fun getTodayTeamList(filter: String?) {
        if (employeeJob?.isActive == true && employeeFilter == filter) return
        employeeJob?.cancel()
        employeeFilter = filter
        employeeJob = viewModelScope.launch {
            blocking(true)
            try {
                when (val result = homeRepo.employees(filter)) {
                    is ApiResult.Success -> employListResponse.value = Event(result.data)
                    is ApiResult.Failure -> homeError.value = Event(result.message)
                }
            } finally { blocking(false) }
        }
    }
}
