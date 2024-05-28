package com.axelliant.android_erp.utils

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.axelliant.android_erp.config.AppConst.SERVER_DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utils {

    fun getServerFormat(
        dateFormat: String = SERVER_DATE_FORMAT,
        date: Date = getCurrentDate()
    ): String {
        val format = SimpleDateFormat(dateFormat, Locale.getDefault())
        return format.format(date)
    }

    private fun getCurrentDate(): Date {
        return Calendar.getInstance().time
    }


    fun getLastWeek(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        return Date(cal.timeInMillis)

    }

    fun getFirstDayOfMonth(): Date {
        val cal = Calendar.getInstance() // this takes current date
        cal[Calendar.DAY_OF_MONTH] = 1
        return Date(cal.timeInMillis)
    }

    fun getLastDayOfMonth(): Date {
        val cal = Calendar.getInstance()
        cal[Calendar.DAY_OF_MONTH] = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return Date(cal.timeInMillis)

    }

    fun getRandomString(): String {
        return (0..10).random().toString()
    }
    fun ConstraintLayout.hideshow(v: View) {
        val show = v.toggleArrow()
        if (show) {
            ViewAnimation.expand(this, object : ViewAnimation.AnimListener {
                override fun onFinish() {
                    this@hideshow.isVisible = true
                    // Toast.makeText(context, "close", Toast.LENGTH_SHORT).show();
                }
            })
        } else {
            ViewAnimation.collapse(this)
        }
    }

    fun View.toggleArrow(): Boolean {
        return if (this.rotation == 0f) {
            this.animate().setDuration(200).rotation(180f)
            true
        } else {
            this.animate().setDuration(200).rotation(0f)
            false
        }
    }

}