package com.axelliant.hris.features.calendar.data.remote

import com.axelliant.hris.features.calendar.data.remote.dto.GraphCalendarViewResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GraphCalendarApiService {
    @GET("v1.0/me/calendarView")
    suspend fun getCalendarView(
        @Header("Authorization") authorization: String,
        @Query("startDateTime") startDateTime: String,
        @Query("endDateTime") endDateTime: String,
        @Query("\$orderby") orderBy: String = "start/dateTime"
    ): Response<GraphCalendarViewResponseDto>
}
