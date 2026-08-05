package com.axelliant.hris.core.contracts.session

data class AppSession(
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val accessToken: String? = null,
    val roles: Set<String> = emptySet(),
    val expiresAtEpochMillis: Long? = null
)
