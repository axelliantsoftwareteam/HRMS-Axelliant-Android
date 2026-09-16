package com.axelliant.hris.core.contracts.auth

import com.axelliant.hris.core.contracts.session.AppSession

enum class AuthProvider {
    MICROSOFT,
    PASSWORD,
    TOKEN
}

data class AuthResult(
    val session: AppSession?,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = session != null && errorMessage == null
}

interface AuthContract {
    suspend fun signIn(provider: AuthProvider): AuthResult
    suspend fun refreshSession(): AuthResult?
    suspend fun signOut()
}
