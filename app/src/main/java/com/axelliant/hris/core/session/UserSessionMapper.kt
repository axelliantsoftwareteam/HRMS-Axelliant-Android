package com.axelliant.hris.core.session

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

internal fun String.toUserSessionFromAccessToken(): UserSession {
    val claims = decodeJwtClaims()
    return UserSession(
        accessToken = this,
        refreshToken = null,
        userId = claims?.optString(CLAIM_USER_ID).orEmpty(),
        displayName = claims?.optString(CLAIM_DISPLAY_NAME).orEmpty(),
        email = claims?.optString(CLAIM_EMAIL).orEmpty().ifBlank {
            claims?.optString(CLAIM_SUBJECT).orEmpty()
        }
    )
}

private fun String.decodeJwtClaims(): JSONObject? {
    return runCatching {
        val payload = split(".").getOrNull(JWT_PAYLOAD_INDEX).orEmpty()
        val decodedBytes = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
    }.getOrNull()
}

private const val JWT_PAYLOAD_INDEX = 1
private const val CLAIM_SUBJECT = "sub"
private const val CLAIM_EMAIL =
    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"
private const val CLAIM_USER_ID = "userId"
private const val CLAIM_DISPLAY_NAME = "displayName"
