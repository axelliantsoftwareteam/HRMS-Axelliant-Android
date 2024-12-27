package com.axelliant.hris.model.resourceManage
import com.axelliant.hris.model.base.Meta

data class GetListProject(
        var project_list: ArrayList<ProjectType>?= arrayListOf(),
        val meta: Meta
)