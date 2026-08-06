package com.axelliant.hris.core.session

interface SessionManager {
    fun saveSession(session: UserSession)
    fun saveMicrosoftGraphToken(token: String)
    fun clearMicrosoftGraphToken()
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getMicrosoftGraphToken(): String?
    fun isLoggedIn(): Boolean
    fun clearSession()
}
