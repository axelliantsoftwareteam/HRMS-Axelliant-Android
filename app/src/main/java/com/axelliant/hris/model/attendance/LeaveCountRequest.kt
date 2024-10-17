package com.axelliant.hris.model.attendance

class LeaveCountRequest {
     var start_date:String=""
     var end_date:String=""
     var filters:List<String>?= null
     var employee_list:List<String> = listOf()
     var for_approvals:Int=0


}