package com.axelliant.hris.core.contracts.auth

import com.axelliant.hris.core.contracts.session.SessionContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HrisAuthContractAdapter @Inject constructor(
    private val sessionContract: SessionContract
) : AuthContract {
    override suspend fun signIn(provider: AuthProvider): AuthResult {
        if (provider == AuthProvider.TOKEN && sessionContract.hasValidSession()) {
            return AuthResult(sessionContract.currentSession())
        }

        return AuthResult(
            session = null,
            errorMessage = "Interactive sign-in is handled by the existing HRIS login flow."
        )
    }

    override suspend fun refreshSession(): AuthResult? {
        val session = sessionContract.currentSession() ?: return null
        return AuthResult(session)
    }

    override suspend fun signOut() {
        sessionContract.clearSession()
    }
}
