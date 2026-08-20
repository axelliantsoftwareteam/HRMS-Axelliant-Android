package com.axelliant.hris.utils

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.axelliant.hris.core.constants.AppDateFormats
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {

    fun getServerFormat(
        dateFormat: String = AppDateFormats.SERVER_DATE,
        date: Date = getCurrentDate()
    ): String {
        val format = SimpleDateFormat(dateFormat, Locale.getDefault())
        return format.format(date)
    }

    private fun getCurrentDate(): Date {
        return Calendar.getInstance().time
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat(AppDateFormats.ATTENDANCE_DATE_TIME)
        return sdf.format(Date())

        /*
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                return sdf.format(Date())*/
    }

    fun formatTitleDate(input: String): String {
        try {
            val inputFormatter = SimpleDateFormat(AppDateFormats.RESOURCE_DATE_TIME, Locale.ENGLISH)
            val date = inputFormatter.parse(input)

            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val formattedDate = date?.let { outputFormatter.format(it) } ?: input

            return " $formattedDate"
        } catch (e: ParseException) {
            e.printStackTrace()
            return input
        }
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

    fun ConstraintLayout.hideShow(v: View) {
        val show = v.toggleArrow()
        if (show) {
            ViewAnimation.expand(this, object : ViewAnimation.AnimListener {
                override fun onFinish() {
                    this@hideShow.isVisible = true
                }
            })
        } else {
            ViewAnimation.collapse(this)
        }
    }

    private fun View.toggleArrow(): Boolean {
        return if (this.rotation == 0f) {
            this.animate().setDuration(200).rotation(180f)
            true
        } else {
            this.animate().setDuration(200).rotation(0f)
            false
        }
    }

    /**
     * MaterialDatePicker returns selection millis at UTC midnight for the
     * calendar day the user tapped. Converting that directly to a Date and
     * formatting in local timezone can roll the day back (e.g. Jul 1 -> Jun 30
     * for timezones ahead of UTC). This pulls out the year/month/day fields
     * the picker visually showed and reconstructs them at local midnight.
     */
    fun utcToLocalDate(utcMillis: Long): Date {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
