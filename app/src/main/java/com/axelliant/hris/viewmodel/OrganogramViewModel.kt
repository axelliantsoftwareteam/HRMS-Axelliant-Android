package com.axelliant.hris.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.team.OrgNode
import com.axelliant.hris.model.team.OrganogramData
import com.axelliant.hris.model.team.TeamNum
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.network.ApiResult

/** Loads the manager's team tree (`get_team_organogram`); the screen reloads on every resume. */
open class OrganogramViewModel(private val api: ApiInterface) : BaseViewModel() {

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


    fun reload() {
        if (listLoading().value?.isLoading == true) return
        val ticket = beginListLoad(kind = ListLoadKind.REFRESH, replace = true)
        requestListPart<OrganogramData>(api.getTeamOrganogram(AppConst.authHeader()), OrganogramData::class.java,
            "list", ticket) { result ->
            when (result) {
                is ApiResult.Success -> {
                    nodes = result.data.nodes.orEmpty()
                    teamCount = TeamNum.int(result.data.team_count) ?: 0
                    hasTeam = TeamNum.bool(result.data.has_team) && nodes.isNotEmpty()
                    hasLoaded = true
                    finishListLoad("list", ticket, if (hasTeam) nodes.size else 0)
                }
                is ApiResult.Failure -> finishListLoad("list", ticket, error = result.message)
            }
            _changed.value = Unit
        }
    }
}