package com.axelliant.hris.core.session

import com.axelliant.hris.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevSessionInitializer @Inject constructor(
    private val sessionManager: SessionManager
) {
    fun initialize() {
        if (BuildConfig.ENVIRONMENT != "local") return

        val baseToken = BuildConfig.API_BASE_TOKEN.trim()
        if (baseToken.isBlank()) return

        sessionManager.saveSession(baseToken.toUserSessionFromAccessToken())
    }
}
