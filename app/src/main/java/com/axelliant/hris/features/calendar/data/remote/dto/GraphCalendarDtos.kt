package com.axelliant.hris.features.calendar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GraphCalendarViewResponseDto(
    @SerializedName("@odata.nextLink") val nextLink: String?,
    val value: List<GraphCalendarEventDto>?
)

data class GraphCalendarEventDto(
    val id: String?,
    val subject: String?,
    val bodyPreview: String?,
    val webLink: String?,
    val onlineMeetingUrl: String?,
    val isAllDay: Boolean?,
    val isOnlineMeeting: Boolean?,
    val showAs: String?,
    val start: GraphDateTimeDto?,
    val end: GraphDateTimeDto?,
    val location: GraphLocationDto?,
    val organizer: GraphRecipientDto?,
    val attendees: List<GraphAttendeeDto>?
)

data class GraphDateTimeDto(
    val dateTime: String?,
    val timeZone: String?
)

data class GraphLocationDto(
    val displayName: String?
)

data class GraphRecipientDto(
    val emailAddress: GraphEmailAddressDto?
)

data class GraphAttendeeDto(
    val emailAddress: GraphEmailAddressDto?,
    val status: GraphResponseStatusDto?
)

data class GraphEmailAddressDto(
    val name: String?,
    val address: String?
)

data class GraphResponseStatusDto(
    val response: String?
)
