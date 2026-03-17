package com.axelliant.hris.model.attendance

import com.axelliant.hris.enums.AttendanceFilter

class AttendanceInput {
     var startDate:String=""
     var endDate:String=""
     var employeeId:List<String> = listOf()
     var filter: AttendanceFilter=AttendanceFilter.WEEK
     var filters:String=""
     var for_approvals:Int=0

}