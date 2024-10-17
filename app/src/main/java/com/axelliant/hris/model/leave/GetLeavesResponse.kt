package com.axelliant.hris.model.leave
import com.axelliant.hris.model.base.Meta

data class GetLeavesResponse(
        var leaves: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)