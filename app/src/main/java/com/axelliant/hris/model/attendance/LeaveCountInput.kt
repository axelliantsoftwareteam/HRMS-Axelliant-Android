package com.axelliant.hris.model.attendance

import com.axelliant.hris.enums.AttendanceFilter

class LeaveCountInput {
     var startDate:String=""
     var endDate:String=""
     var employeeId:List<String> = listOf()
     var filter: AttendanceFilter=AttendanceFilter.WEEK
     var filters:List<String>?=null
     var for_approvals:Int=0

}