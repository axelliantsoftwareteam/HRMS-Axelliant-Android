package com.axelliant.hris.model.leave
import com.axelliant.hris.model.base.Meta

data class GetAttendanceResponse(
        var checkin: ArrayList<SpinnerType>?= arrayListOf(),
        var location: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)