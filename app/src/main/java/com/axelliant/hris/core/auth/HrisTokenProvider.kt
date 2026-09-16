package com.axelliant.hris.core.auth

interface HrisTokenProvider {
    fun rawToken(): String
    fun authorizationHeader(): String
    fun saveToken(token: String)
    fun clearToken()
}
