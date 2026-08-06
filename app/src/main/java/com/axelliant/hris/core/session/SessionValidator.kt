package com.axelliant.hris.core.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionValidator @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenValidator: TokenValidator
) {
    fun hasValidSession(): Boolean {
        val isValid = tokenValidator.isTokenValid(sessionManager.getAccessToken())
        if (!isValid) {
            sessionManager.clearSession()
        }
        return isValid
    }
}
