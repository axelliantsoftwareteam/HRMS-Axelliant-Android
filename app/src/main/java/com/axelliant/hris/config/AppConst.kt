package com.axelliant.hris.config

import com.axelliant.hris.observable.ObservableCode

object AppConst {

/*
    var TOKEN: String? = "17cdde0cded53dd:84d1ed09630ae50"
*/
    var TOKEN: String? = ""

//    const val KEY_PARAM  = "key"
    const val KEY_ID  = "employeeId"
    const val LeaveRequestParam  = "leaveRequest"
    const val AttendanceRequestParam  = "attendanceRequest"
    const val RequestType = "requestType"
    val observableCode = ObservableCode()

//    const val DATE_FORMAT =  "dd MMM"
//    const val DATE_END_FORMAT =  "dd MMM,yyyy"
    const val SERVER_DATE_FORMAT =  "yyyy-MM-dd"
    const val SERVER_DATE_FORMAT_ATTENDANCE =  "yyyy-MM-dd"
    const val ATTENDANCE_DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss"
    const val RESOURCE_DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss.SSSSSS"
//    const val DISPLAY_TIME_FORMAT =  "HH:mm"

    const val ExpenseRequestParam  = "expenseRequest"
    const val ExpenseRequestIDParam  = "expenseId"
    const val ExpenseRequestAttachments  = "expenseAttachments"

    const val HoursRequestIDParam  = "hoursId"
    const val HoursRequestParam  = "hoursRequest"





    // Define the input format
//    val inputFormat = SimpleDateFormat(AppConst.ATTENDANCE_DATE_FORMAT, Locale.getDefault())
//
//    // Define the output format
//    val outputFormat = SimpleDateFormat(AppConst.DISPLAY_TIME_FORMAT, Locale.getDefault())
//
//
//    val upComingLeavesinputFormat = SimpleDateFormat(AppConst.SERVER_DATE_FORMAT, Locale.getDefault())
//    val upComingLeavesoutputFormat = SimpleDateFormat(AppConst.DATE_FORMAT, Locale.getDefault())
//    val upComingLeavesoutputEndFormat = SimpleDateFormat(AppConst.DATE_END_FORMAT, Locale.getDefault())
//


}