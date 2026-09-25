package com.axelliant.hris.core.contracts.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.network.ApiErrorMessages
import kotlinx.coroutines.flow.SharedFlow

data class SessionExpiredEvent(
    val workspace: WorkspaceKey,
    val message: String
)

interface SessionExpiryContract {
    val sessionExpiredEvents: SharedFlow<SessionExpiredEvent>

    fun handleSessionExpired(
        workspace: WorkspaceKey,
        message: String = ApiErrorMessages.UNAUTHORIZED
    )
}
