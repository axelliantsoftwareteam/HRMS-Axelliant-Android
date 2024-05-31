package com.axelliant.hrms.model.leave
import com.axelliant.hrms.model.base.Meta

data class GetAttendanceResponse(
        var checkin: ArrayList<SpinnerType>?= arrayListOf(),
        var location: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)