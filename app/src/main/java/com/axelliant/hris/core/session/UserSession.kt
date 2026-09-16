package com.axelliant.hris.core.session

data class UserSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
    val displayName: String? = null,
    val email: String? = null
)
