package com.axelliant.hris.core.network

import com.axelliant.hris.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getAccessToken()
        val request = chain.request().newBuilder().apply {
            header("Accept", "application/json")
            if (!token.isNullOrBlank() && chain.request().header("Authorization").isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()

        return chain.proceed(request)
    }
}
