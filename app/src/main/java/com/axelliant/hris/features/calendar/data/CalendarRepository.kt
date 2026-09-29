package com.axelliant.hris.features.calendar.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.features.calendar.data.remote.GraphCalendarApiService
import com.axelliant.hris.features.calendar.data.remote.dto.GraphCalendarEventDto
import com.axelliant.hris.features.calendar.domain.model.CalendarEventUiModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val apiService: GraphCalendarApiService,
    private val sessionManager: SessionManager,
    private val safeApiExecutor: SafeApiExecutor
) {
    suspend fun getCalendarView(
        startDateTime: String,
        endDateTime: String
    ): ApiResult<List<CalendarEventUiModel>> {
        val token = sessionManager.getMicrosoftGraphToken()?.takeIf { it.isNotBlank() }
            ?: return ApiResult.Unauthorized

        return when (val result = safeApiExecutor.execute(handleUnauthorized = false) {
            apiService.getCalendarView(
                authorization = "Bearer $token",
                startDateTime = startDateTime,
                endDateTime = endDateTime
            )
        }) {
            is ApiResult.Success -> ApiResult.Success(result.data.value.orEmpty().mapIndexed { index, dto ->
                dto.toUiModel(index)
            })
            ApiResult.Empty -> ApiResult.Success(emptyList())
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun GraphCalendarEventDto.toUiModel(index: Int): CalendarEventUiModel {
        val parsedStart = start?.dateTime.parseGraphDate()
        val parsedEnd = end?.dateTime.parseGraphDate()
        val locationText = location?.displayName.orEmpty()
        val teamsMeeting = isOnlineMeeting == true ||
            onlineMeetingUrl?.isNotBlank() == true ||
            locationText.contains("teams", ignoreCase = true)
        val fallbackSubtitle = when {
            teamsMeeting -> "Microsoft Teams Meeting"
            locationText.isNotBlank() -> locationText
            else -> "Outlook calendar event"
        }

        return CalendarEventUiModel(
            id = id.orEmpty().ifBlank { "calendar-event-$index" },
            title = subject.orEmpty().ifBlank { "Untitled event" },
            subtitle = fallbackSubtitle,
            dateLabel = parsedStart.formatDate("EEEE, MMMM d, yyyy"),
            dayLabel = parsedStart.formatDate("EEE\nMMM d"),
            timeRange = if (isAllDay == true) {
                "All day"
            } else {
                "${parsedStart.formatDate("h:mm a")} - ${parsedEnd.formatDate("h:mm a")}"
            },
            startDate = parsedStart,
            endDate = parsedEnd,
            isAllDay = isAllDay == true,
            isTeamsMeeting = teamsMeeting,
            location = locationText.ifBlank { fallbackSubtitle },
            organizerName = organizer?.emailAddress?.name.orEmpty().ifBlank { "--" },
            organizerEmail = organizer?.emailAddress?.address.orEmpty(),
            attendeesSummary = attendees.orEmpty().mapNotNull { it.emailAddress?.name?.takeIf(String::isNotBlank) }
                .take(3)
                .joinToString(", ")
                .ifBlank { "No attendees" },
            description = bodyPreview.orEmpty().ifBlank { "No description" },
            webLink = webLink,
            onlineMeetingUrl = onlineMeetingUrl,
            accentColor = EVENT_COLORS[index % EVENT_COLORS.size]
        )
    }

    private fun String?.parseGraphDate(): Date? {
        if (isNullOrBlank()) return null
        INPUT_FORMATS.forEach { pattern ->
            try {
                return SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(this)
            } catch (_: ParseException) {
            }
        }
        return null
    }

    private fun Date?.formatDate(pattern: String): String {
        if (this == null) return "--"
        return SimpleDateFormat(pattern, Locale.US).format(this)
    }

    private companion object {
        val INPUT_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        val EVENT_COLORS = listOf(
            0xFF21A366.toInt(),
            0xFFE76F00.toInt(),
            0xFF6264A7.toInt(),
            0xFF0078D4.toInt(),
            0xFF8E24AA.toInt()
        )
    }
}
