package com.axelliant.android_erp.model.attendance

import com.axelliant.android_erp.enums.AttendanceFilter

class AttendanceInput {
     var startDate:String=""
     var endDate:String=""
     var employeeId:List<String> = listOf()
     var filter: AttendanceFilter=AttendanceFilter.WEEK

}