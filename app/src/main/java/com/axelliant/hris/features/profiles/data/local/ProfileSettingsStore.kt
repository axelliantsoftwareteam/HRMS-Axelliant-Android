package com.axelliant.hris.features.profiles.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getSettings(): ProfileLocalSettings {
        return ProfileLocalSettings(
            timeZoneId = prefs.getString(KEY_TIME_ZONE, null)
                ?: TimeZone.getDefault().id,
            dateFormat = prefs.getString(KEY_DATE_FORMAT, null)
                ?: DEFAULT_DATE_FORMAT,
            timeFormat = prefs.getString(KEY_TIME_FORMAT, null)
                ?: DEFAULT_TIME_FORMAT
        )
    }

    fun saveTimeZone(timeZoneId: String): ProfileLocalSettings {
        prefs.edit().putString(KEY_TIME_ZONE, timeZoneId).apply()
        return getSettings()
    }

    fun saveDateFormat(dateFormat: String): ProfileLocalSettings {
        prefs.edit().putString(KEY_DATE_FORMAT, dateFormat).apply()
        return getSettings()
    }

    fun saveTimeFormat(timeFormat: String): ProfileLocalSettings {
        prefs.edit().putString(KEY_TIME_FORMAT, timeFormat).apply()
        return getSettings()
    }

    companion object {
        const val DEFAULT_DATE_FORMAT = "MM/DD/YYYY"
        const val DEFAULT_TIME_FORMAT = "12-Hour (AM/PM)"
        val TIME_ZONE_OPTIONS = listOf(
            "Asia/Karachi",
            "UTC",
            "Asia/Dubai",
            "Europe/London",
            "America/New_York"
        )
        val DATE_FORMAT_OPTIONS = listOf("MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD")
        val TIME_FORMAT_OPTIONS = listOf("12-Hour (AM/PM)", "24-Hour")

        private const val PREF_NAME = "profile_local_settings"
        private const val KEY_TIME_ZONE = "time_zone"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_TIME_FORMAT = "time_format"
    }
}

data class ProfileLocalSettings(
    val timeZoneId: String,
    val dateFormat: String,
    val timeFormat: String
) {
    val timeZoneOffsetLabel: String
        get() {
            val timeZone = TimeZone.getTimeZone(timeZoneId)
            val offsetMinutes = timeZone.getOffset(System.currentTimeMillis()) / MILLIS_PER_MINUTE
            val sign = if (offsetMinutes >= 0) "+" else "-"
            val absoluteMinutes = kotlin.math.abs(offsetMinutes)
            val hours = absoluteMinutes / MINUTES_PER_HOUR
            val minutes = absoluteMinutes % MINUTES_PER_HOUR
            return "(GMT$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')})"
        }

    companion object {
        private const val MILLIS_PER_MINUTE = 60 * 1000
        private const val MINUTES_PER_HOUR = 60
    }
}
