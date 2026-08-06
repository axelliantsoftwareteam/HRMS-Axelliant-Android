package com.axelliant.hris.core.auth

import com.axelliant.hris.utils.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHrisTokenProvider @Inject constructor(
    private val sessionManager: SessionManager
) : HrisTokenProvider {
    override fun rawToken(): String {
        return sessionManager.getToken().orEmpty()
    }

    override fun authorizationHeader(): String {
        return "token ${rawToken()}"
    }

    override fun saveToken(token: String) {
        sessionManager.saveToken(token)
    }

    override fun clearToken() {
        sessionManager.logoutUser()
    }
}
