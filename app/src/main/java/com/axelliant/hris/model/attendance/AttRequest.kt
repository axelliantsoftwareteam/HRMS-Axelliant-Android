package com.axelliant.hris.model.attendance

class AttRequest {
     var start_date:String=""
     var end_date:String=""
     var filters:String=""
     var employee_list:List<String> = listOf()
     var for_approvals:Int=0


}