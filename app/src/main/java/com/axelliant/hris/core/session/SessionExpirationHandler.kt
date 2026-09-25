package com.axelliant.hris.core.session

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.SessionExpiredEvent
import com.axelliant.hris.core.contracts.session.SessionExpiryContract
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class SessionExpirationHandler @Inject constructor(
    private val workspaceSessionProvider: WorkspaceSessionProvider
) : SessionExpiryContract {
    private val _sessionExpiredEvents = MutableSharedFlow<SessionExpiredEvent>(extraBufferCapacity = 1)
    override val sessionExpiredEvents: SharedFlow<SessionExpiredEvent> = _sessionExpiredEvents.asSharedFlow()

    override fun handleSessionExpired(workspace: WorkspaceKey, message: String) {
        workspaceSessionProvider.clearAllSessions()
        _sessionExpiredEvents.tryEmit(
            SessionExpiredEvent(
                workspace = workspace,
                message = message
            )
        )
    }
}
