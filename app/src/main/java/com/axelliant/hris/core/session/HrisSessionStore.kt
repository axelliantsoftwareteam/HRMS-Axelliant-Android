package com.axelliant.hris.core.session

import com.axelliant.hris.core.contracts.session.AppSession

interface HrisSessionStore {
    fun hasValidSession(): Boolean
    fun currentSession(): AppSession?
    fun saveMicrosoftSession(email: String?, accessToken: String)
    fun rawToken(): String
    fun saveToken(token: String)
    fun clearSession()
}
