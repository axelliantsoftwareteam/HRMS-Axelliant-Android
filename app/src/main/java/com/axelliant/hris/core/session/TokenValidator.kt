package com.axelliant.hris.core.session

import android.util.Base64
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenValidator @Inject constructor() {

    fun isTokenValid(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val segments = token.split(".")
        if (segments.size != 3) return false

        val payload = decodeJwtPayload(segments[1]) ?: return false
        val expiresAt = payload.get("exp")?.asLong ?: return false
        val nowInSeconds = System.currentTimeMillis() / 1000
        return expiresAt > nowInSeconds
    }

    private fun decodeJwtPayload(payloadSegment: String): com.google.gson.JsonObject? {
        return runCatching {
            val decodedBytes = Base64.decode(
                payloadSegment,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            JsonParser.parseString(String(decodedBytes)).asJsonObject
        }.getOrNull()
    }
}
