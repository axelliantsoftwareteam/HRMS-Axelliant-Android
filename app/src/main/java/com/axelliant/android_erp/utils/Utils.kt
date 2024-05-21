
package com.axelliant.android_erp.utils

import com.axelliant.android_erp.config.AppConst.DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    fun getCurrentDate(): String {
        val date: Date = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(date)
    }
    fun getRandomString():String{
        return   (0..10).random().toString()
    }

}