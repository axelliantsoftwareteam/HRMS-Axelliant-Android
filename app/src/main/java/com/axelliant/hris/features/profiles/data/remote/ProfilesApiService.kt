package com.axelliant.hris.features.profiles.data.remote

import com.axelliant.hris.features.profiles.data.remote.dto.MicrosoftProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ProfilesApiService {
    @GET("https://graph.microsoft.com/v1.0/me")
    suspend fun getCurrentMicrosoftProfile(
        @Header("Authorization") authorization: String,
        @Query("\$select") select: String = MICROSOFT_PROFILE_FIELDS
    ): Response<MicrosoftProfileResponse>

    companion object {
        const val MICROSOFT_PROFILE_FIELDS =
            "displayName,mail,department,jobTitle,companyName,businessPhones,mobilePhone,officeLocation"
    }
}
