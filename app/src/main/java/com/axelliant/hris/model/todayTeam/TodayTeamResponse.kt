package com.axelliant.hris.model.todayTeam

import com.axelliant.hris.model.base.Meta

data class TodayTeamResponse(
    val in_office: Int?=null,
    val work_from_home: Int?=null,
    val checkin_count: Int?=null,
    val checkout_count: Int?=null,
    val leave_count: Int?=null,
    val absent_count: Int?=null,
    val team_member_count: Int?=null,
    val meta: Meta
)