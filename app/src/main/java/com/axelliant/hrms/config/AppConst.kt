package com.axelliant.hrms.config

import com.axelliant.hrms.observable.ObservableCode
import java.text.SimpleDateFormat
import java.util.Locale

object AppConst {

    var TOKEN: String? = null
    const val KEY_PARAM  = "key"
    const val KEY_ID  = "employeeId"
    val observableCode = ObservableCode()

    const val DATE_FORMAT =  "dd MMM,yyyy"
    const val SERVER_DATE_FORMAT =  "yyyy-MM-dd"
    const val SERVER_DATE_FORMAT_ATTENDANCE =  "yyyy-MM-dd HH:mm:ss"
    const val ATTENDANCE_DATE_FORMAT =  "dd-MM-yyyy HH:mm:ss"
    const val DISPLAY_TIME_FORMAT =  "HH:mm"
    // Define the input format
    val inputFormat = SimpleDateFormat(AppConst.ATTENDANCE_DATE_FORMAT, Locale.getDefault())

    // Define the output format
    val outputFormat = SimpleDateFormat(AppConst.DISPLAY_TIME_FORMAT, Locale.getDefault())



}