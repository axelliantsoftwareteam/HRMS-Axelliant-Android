package com.axelliant.hris.features.auth.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.auth.data.remote.dto.LoginRequest
import com.axelliant.hris.features.auth.data.remote.dto.LoginResponse
import com.axelliant.hris.features.auth.data.remote.dto.MicrosoftTokenData
import com.axelliant.hris.features.auth.data.remote.dto.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/GetToken")
    suspend fun getMicrosoftToken(
        @Query("token") token: String
    ): Response<BaseApiModel<MicrosoftTokenData>>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserProfileResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
