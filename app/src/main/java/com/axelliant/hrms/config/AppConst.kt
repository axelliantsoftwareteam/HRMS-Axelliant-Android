package com.axelliant.hrms.config

import com.axelliant.hrms.observable.ObservableCode
import java.net.Authenticator.RequestorType
import java.text.SimpleDateFormat
import java.util.Locale

object AppConst {

    var TOKEN: String? = null
    const val KEY_PARAM  = "key"
    const val KEY_ID  = "employeeId"
    const val LeaveRequestParam  = "leaveRequest"
    const val AttendanceRequestParam  = "attendanceRequest"
    const val RequestType = "requestType"
    val observableCode = ObservableCode()

    const val DATE_FORMAT =  "dd MMM"
    const val DATE_END_FORMAT =  "dd MMM,yyyy"
    const val SERVER_DATE_FORMAT =  "yyyy-MM-dd"
    const val SERVER_DATE_FORMAT_ATTENDANCE =  "yyyy-MM-dd"
    const val ATTENDANCE_DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss"
    const val DISPLAY_TIME_FORMAT =  "HH:mm"

    const val ExpenseRequestParam  = "expenseRequest"



    // Define the input format
    val inputFormat = SimpleDateFormat(AppConst.ATTENDANCE_DATE_FORMAT, Locale.getDefault())

    // Define the output format
    val outputFormat = SimpleDateFormat(AppConst.DISPLAY_TIME_FORMAT, Locale.getDefault())


    val upComingLeavesinputFormat = SimpleDateFormat(AppConst.SERVER_DATE_FORMAT, Locale.getDefault())
    val upComingLeavesoutputFormat = SimpleDateFormat(AppConst.DATE_FORMAT, Locale.getDefault())
    val upComingLeavesoutputEndFormat = SimpleDateFormat(AppConst.DATE_END_FORMAT, Locale.getDefault())



}