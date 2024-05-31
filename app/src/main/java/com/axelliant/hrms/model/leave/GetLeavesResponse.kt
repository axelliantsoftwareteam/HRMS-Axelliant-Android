package com.axelliant.hrms.model.leave
import com.axelliant.hrms.model.base.Meta

data class GetLeavesResponse(
        var leaves: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)