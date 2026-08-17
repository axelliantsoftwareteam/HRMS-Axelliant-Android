package com.axelliant.hris.features.commonlogin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.R
import com.axelliant.hris.core.auth.GlobalLogoutCoordinator
import com.axelliant.hris.core.auth.PendingCommonLoginStore
import com.axelliant.hris.core.contracts.auth.AuthSessionResult
import com.axelliant.hris.core.contracts.auth.WorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.features.auth.data.local.LoginCredentialStore
import com.axelliant.hris.features.auth.data.remote.dto.LoginValidationState
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class CommonLoginMethod {
    EXISTING_SESSION,
    PASSWORD,
    MICROSOFT_TOKEN
}

data class CommonLoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val validation: LoginValidationState = LoginValidationState(),
    val isLoginEnabled: Boolean = false,
    val loginMethod: CommonLoginMethod = CommonLoginMethod.EXISTING_SESSION,
    val isLoading: Boolean = false,
    val isMicrosoftLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val session: AppSession? = null,
    val allowedWorkspaces: Set<WorkspaceKey> = emptySet(),
    val errorMessage: String? = null,
    val errorMessageRes: Int? = null
)

@HiltViewModel
class CommonLoginViewModel @Inject constructor(
    private val authSessionRepositoryProvider: WorkspaceAuthSessionRepositoryProvider,
    private val pendingCommonLoginStore: PendingCommonLoginStore,
    private val loginPreferences: LoginCredentialStore,
    private val microsoftAuthManager: MicrosoftAuthManager,
    private val globalLogoutCoordinator: GlobalLogoutCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommonLoginUiState(
            email = loginPreferences.getRememberedEmail(),
            password = loginPreferences.getRememberedPassword(),
            rememberMe = loginPreferences.isRememberMeEnabled()
        ).validate()
    )
    val uiState: StateFlow<CommonLoginUiState> = _uiState.asStateFlow()

    init {
        restoreExistingSessions()
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null,
            errorMessageRes = null
        ).validate().also(::saveRememberedCredentialsIfEnabled)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null,
            errorMessageRes = null
        ).validate().also(::saveRememberedCredentialsIfEnabled)
    }

    fun onRememberMeChanged(isChecked: Boolean) {
        val state = _uiState.value.copy(rememberMe = isChecked)
        _uiState.value = state
        if (isChecked) {
            saveRememberedCredentialsIfEnabled(state)
        } else {
            loginPreferences.clearRememberedCredentials()
        }
    }

    fun restoreExistingSessions() {
        val activeWorkspaces = WorkspaceKey.values()
            .filter { workspace -> authSessionRepositoryProvider.repositoryFor(workspace).hasValidSession() }
            .toSet()
        val session = activeWorkspaces.firstOrNull()?.let { workspace ->
            authSessionRepositoryProvider.repositoryFor(workspace).currentSession()
        }

        pendingCommonLoginStore.markExistingSessionAuthenticated(activeWorkspaces)
        _uiState.value = _uiState.value.copy(
            loginMethod = CommonLoginMethod.EXISTING_SESSION,
            isLoading = false,
            isMicrosoftLoading = false,
            isAuthenticated = activeWorkspaces.isNotEmpty(),
            session = session,
            allowedWorkspaces = activeWorkspaces,
            errorMessage = null,
            errorMessageRes = null
        )
    }

    fun signInWithInternalAppsPassword() {
        val validatedState = _uiState.value.validate(forceErrors = true)
        _uiState.value = validatedState
        if (!validatedState.isLoginEnabled) return

        authenticate(
            workspace = WorkspaceKey.INTERNAL_APPS,
            loginMethod = CommonLoginMethod.PASSWORD
        ) {
            authSessionRepositoryProvider.repositoryFor(WorkspaceKey.INTERNAL_APPS)
                .signInWithPassword(validatedState.email.trim(), validatedState.password)
        }
    }

    fun acceptMicrosoftToken(idToken: String, graphAccessToken: String? = null) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                loginMethod = CommonLoginMethod.MICROSOFT_TOKEN,
                isMicrosoftLoading = false,
                errorMessageRes = R.string.ia_login_microsoft_unavailable
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loginMethod = CommonLoginMethod.MICROSOFT_TOKEN,
                isLoading = false,
                isMicrosoftLoading = true,
                isAuthenticated = false,
                session = null,
                allowedWorkspaces = emptySet(),
                errorMessage = null,
                errorMessageRes = null
            )

            val results = signInMicrosoftWorkspaces(idToken, graphAccessToken)
            val successfulWorkspaces = results
                .filterValues { result -> result.isSuccess }
                .keys
            val firstSession = results.values.firstNotNullOfOrNull { result -> result.session }

            if (WorkspaceKey.INTERNAL_APPS in successfulWorkspaces) {
                pendingCommonLoginStore.markExistingSessionAuthenticated(successfulWorkspaces)
                _uiState.value = _uiState.value.copy(
                    isMicrosoftLoading = false,
                    isAuthenticated = true,
                    session = firstSession,
                    allowedWorkspaces = successfulWorkspaces,
                    errorMessage = null,
                    errorMessageRes = null
                )
            } else {
                authSessionRepositoryProvider.repositoryFor(WorkspaceKey.HRIS).clearSession()
                authSessionRepositoryProvider.repositoryFor(WorkspaceKey.INTERNAL_APPS).clearSession()
                pendingCommonLoginStore.clear()
                microsoftAuthManager.signOut()
                _uiState.value = _uiState.value.copy(
                    isMicrosoftLoading = false,
                    isAuthenticated = false,
                    session = null,
                    allowedWorkspaces = emptySet(),
                    errorMessage = results.toMicrosoftFailureMessage(),
                    errorMessageRes = null
                )
            }
        }
    }

    fun onMicrosoftLoginStarted() {
        _uiState.value = _uiState.value.copy(
            isMicrosoftLoading = true,
            errorMessage = null,
            errorMessageRes = null
        )
    }

    fun onMicrosoftLoginCancelled() {
        _uiState.value = _uiState.value.copy(
            isMicrosoftLoading = false,
            errorMessageRes = R.string.ia_login_microsoft_cancelled
        )
    }

    fun onMicrosoftLoginFailed(message: String? = null) {
        _uiState.value = _uiState.value.copy(
            isMicrosoftLoading = false,
            errorMessage = message?.takeIf { it.isNotBlank() },
            errorMessageRes = if (message.isNullOrBlank()) {
                R.string.ia_login_microsoft_unavailable
            } else {
                null
            }
        )
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            globalLogoutCoordinator.logout()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isMicrosoftLoading = false,
                isAuthenticated = false,
                session = null,
                allowedWorkspaces = emptySet(),
                errorMessage = null,
                errorMessageRes = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, errorMessageRes = null)
    }

    private fun authenticate(
        workspace: WorkspaceKey,
        loginMethod: CommonLoginMethod,
        authCall: suspend () -> AuthSessionResult
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loginMethod = loginMethod,
                isLoading = true,
                isMicrosoftLoading = false,
                errorMessage = null,
                errorMessageRes = null
            )

            val result = authCall()
            val allowedWorkspaces = if (result.isSuccess) setOf(workspace) else emptySet()
            if (result.isSuccess && workspace == WorkspaceKey.INTERNAL_APPS) {
                pendingCommonLoginStore.markInternalAppsPasswordAuthenticated()
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = result.isSuccess,
                session = result.session,
                allowedWorkspaces = allowedWorkspaces,
                errorMessage = result.errorMessage ?: result.failureMessage()
            )
        }
    }

    private suspend fun signInMicrosoftWorkspaces(
        idToken: String,
        graphAccessToken: String?
    ): Map<WorkspaceKey, AuthSessionResult> = coroutineScope {
        WorkspaceKey.values()
            .map { workspace ->
                async {
                    val result = authSessionRepositoryProvider.repositoryFor(workspace)
                        .signInWithMicrosoftToken(idToken, graphAccessToken)
                    workspace to result
                }
            }
            .associate { deferred -> deferred.await() }
    }

    private fun Map<WorkspaceKey, AuthSessionResult>.toMicrosoftFailureMessage(): String {
        return entries.joinToString(separator = "\n") { (workspace, result) ->
            "${workspace.displayName()}: ${result.errorMessage ?: "Login failed."}"
        }.ifBlank { "Microsoft login failed." }
    }

    private fun WorkspaceKey.displayName(): String {
        return when (this) {
            WorkspaceKey.HRIS -> "HRIS"
            WorkspaceKey.INTERNAL_APPS -> "Internal Apps"
        }
    }

    private fun AuthSessionResult.failureMessage(): String? {
        return if (isSuccess) null else "Login failed."
    }

    private fun CommonLoginUiState.validate(forceErrors: Boolean = false): CommonLoginUiState {
        val normalizedEmail = email.trim()
        val normalizedPassword = password.trim()
        val shouldShowEmailError = forceErrors || normalizedEmail.isNotEmpty()
        val shouldShowPasswordError = forceErrors || normalizedPassword.isNotEmpty()
        val emailError = when {
            normalizedEmail.isEmpty() && shouldShowEmailError -> R.string.ia_login_email_required
            normalizedEmail.isNotEmpty() && !EMAIL_REGEX.matches(normalizedEmail) ->
                R.string.ia_login_email_invalid

            else -> null
        }
        val passwordError = when {
            normalizedPassword.isEmpty() && shouldShowPasswordError -> R.string.ia_login_password_required
            normalizedPassword.isNotEmpty() && !PASSWORD_REGEX.matches(normalizedPassword) ->
                R.string.ia_login_password_invalid

            else -> null
        }

        return copy(
            validation = LoginValidationState(
                emailErrorRes = emailError,
                passwordErrorRes = passwordError
            ),
            isLoginEnabled = normalizedEmail.isNotEmpty() &&
                    normalizedPassword.isNotEmpty() &&
                    emailError == null &&
                    passwordError == null
        )
    }

    private fun saveRememberedCredentialsIfEnabled(state: CommonLoginUiState) {
        if (!state.rememberMe) return

        loginPreferences.saveRememberedCredentials(
            email = state.email.trim(),
            password = state.password
        )
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", RegexOption.IGNORE_CASE)
        val PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$")
    }
}
