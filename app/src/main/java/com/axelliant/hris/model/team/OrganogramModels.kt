package com.axelliant.hris.model.team

/** One person in the team tree from `get_team_organogram`, with the people who report to them. */
data class OrgNode(
    val name: String? = null,
    val title: String? = null,
    val image: String? = null,
    val childs: List<OrgNode>? = null
)

/** `get_team_organogram` payload. */
data class OrganogramData(
    val nodes: List<OrgNode>? = null,
    val team_count: Any? = null,
    val has_team: Any? = null
)
