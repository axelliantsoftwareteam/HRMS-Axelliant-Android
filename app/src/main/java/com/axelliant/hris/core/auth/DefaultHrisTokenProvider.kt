package com.axelliant.hris.core.auth

import com.axelliant.hris.core.session.HrisSessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHrisTokenProvider @Inject constructor(
    private val hrisSessionStore: HrisSessionStore
) : HrisTokenProvider {
    override fun rawToken(): String {
        return hrisSessionStore.rawToken()
    }

    override fun authorizationHeader(): String {
        return "token ${rawToken()}"
    }

    override fun saveToken(token: String) {
        hrisSessionStore.saveToken(token)
    }

    override fun clearToken() {
        hrisSessionStore.clearSession()
    }
}
