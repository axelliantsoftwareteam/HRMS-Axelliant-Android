package com.axelliant.hris.model.todayTeam

import com.axelliant.hris.model.base.Meta

data class EmployProfileListModel(
    val employee_list: ArrayList<EmployTeamProfile>?=null,
    val meta: Meta
)