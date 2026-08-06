package com.axelliant.hris.features.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String,
    val expiresInMins: Int = 60
)

data class LoginResponse(
    val data: LoginEnvelope? = null,
    val message: LoginMessage? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val image: String? = null
)

data class LoginEnvelope(
    val success: Boolean = false,
    val data: LoginTokenData? = null,
    val message: String? = null
)

data class LoginTokenData(
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = null,
    @SerializedName("aE_access_token")
    val aeAccessToken: String? = null,
    @SerializedName("aE_expires_in")
    val aeExpiresIn: String? = null
)

data class LoginMessage(
    val text: String? = null,
    val title: String? = null,
    val messageTypeId: String? = null
)

data class UserProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val image: String? = null
)

data class MicrosoftTokenData(
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("expires_in")
    val expiresIn: Long? = null,
    @SerializedName("aE_access_token")
    val aeAccessToken: String? = null,
    @SerializedName("aE_expires_in")
    val aeExpiresIn: String? = null
)
