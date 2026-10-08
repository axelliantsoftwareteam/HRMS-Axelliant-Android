package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.event.Event
import com.axelliant.hris.model.team.OrgNode
import com.axelliant.hris.model.team.OrganogramData
import com.axelliant.hris.model.team.TeamNum
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult
import com.axelliant.hris.network.enqueueResult

/** Loads the manager's team tree (`get_team_organogram`); the screen reloads on every resume. */
class OrganogramViewModel(private val api: ApiInterface) : BaseViewModel() {

    var nodes: List<OrgNode> = emptyList()
        private set
    var teamCount = 0
        private set
    var hasTeam = false
        private set
    var hasLoaded = false
        private set

    private val _changed = MutableLiveData<Unit>()
    val changed: LiveData<Unit> get() = _changed

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> get() = _error

    private var loading = false

    fun reload() {
        if (loading) return
        loading = true
        val showOverlay = !hasLoaded
        if (showOverlay) isLoading.value = Event(true)
        api.getTeamOrganogram(AppConst.authHeader())
            .enqueueResult<OrganogramData>(OrganogramData::class.java) { result ->
                loading = false
                if (showOverlay) isLoading.value = Event(false)
                hasLoaded = true
                when (result) {
                    is ApiResult.Success -> {
                        nodes = result.data.nodes.orEmpty()
                        teamCount = TeamNum.int(result.data.team_count) ?: 0
                        hasTeam = TeamNum.bool(result.data.has_team) && nodes.isNotEmpty()
                    }
                    is ApiResult.Failure -> {
                        hasTeam = false
                        _error.value = Event("We could not load the organogram right now.")
                    }
                }
                _changed.value = Unit
            }
    }
}
