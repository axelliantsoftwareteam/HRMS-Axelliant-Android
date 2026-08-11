package com.axelliant.hris.features.commonlogin.presentation

import com.axelliant.hris.core.auth.PendingCommonLoginStore
import com.axelliant.hris.core.contracts.auth.AuthSessionRepository
import com.axelliant.hris.core.contracts.auth.AuthSessionResult
import com.axelliant.hris.core.contracts.auth.WorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.session.AppSession
import com.axelliant.hris.features.auth.data.local.LoginCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommonLoginViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoreExistingSessions_enablesOnlyWorkspacesWithSessions() {
        val hrisRepository = FakeAuthSessionRepository(WorkspaceKey.HRIS)
        val internalAppsRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.INTERNAL_APPS,
            session = AppSession(accessToken = "internal-token")
        )
        val pendingStore = PendingCommonLoginStore()
        val viewModel = createViewModel(hrisRepository, internalAppsRepository, pendingStore)

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals(setOf(WorkspaceKey.INTERNAL_APPS), state.allowedWorkspaces)
        assertTrue(pendingStore.canEnter(WorkspaceKey.INTERNAL_APPS))
        assertFalse(pendingStore.canEnter(WorkspaceKey.HRIS))
    }

    @Test
    fun signInWithInternalAppsPassword_callsOnlyInternalAppsBackend() = runTestWithMain {
        val hrisRepository = FakeAuthSessionRepository(WorkspaceKey.HRIS)
        val internalAppsSession = AppSession(accessToken = "internal-password-token")
        val internalAppsRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.INTERNAL_APPS,
            passwordResult = AuthSessionResult(session = internalAppsSession)
        )
        val pendingStore = PendingCommonLoginStore()
        val viewModel = createViewModel(hrisRepository, internalAppsRepository, pendingStore)

        viewModel.onEmailChanged("user@axelliant.com")
        viewModel.onPasswordChanged("Password#1234")
        viewModel.signInWithInternalAppsPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            listOf("user@axelliant.com" to "Password#1234"),
            internalAppsRepository.passwordCalls
        )
        assertTrue(hrisRepository.passwordCalls.isEmpty())
        assertTrue(state.isAuthenticated)
        assertEquals(setOf(WorkspaceKey.INTERNAL_APPS), state.allowedWorkspaces)
        assertEquals(CommonLoginMethod.PASSWORD, state.loginMethod)
        assertTrue(pendingStore.canEnter(WorkspaceKey.INTERNAL_APPS))
        assertFalse(pendingStore.canEnter(WorkspaceKey.HRIS))
    }

    @Test
    fun acceptMicrosoftToken_callsBothBackendsAndEnablesSuccessfulWorkspaces() = runTestWithMain {
        val hrisSession = AppSession(accessToken = "hris-microsoft-token")
        val internalAppsSession = AppSession(accessToken = "internal-microsoft-token")
        val hrisRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.HRIS,
            microsoftResult = AuthSessionResult(session = hrisSession)
        )
        val internalAppsRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.INTERNAL_APPS,
            microsoftResult = AuthSessionResult(session = internalAppsSession)
        )
        val pendingStore = PendingCommonLoginStore()
        val viewModel = createViewModel(hrisRepository, internalAppsRepository, pendingStore)

        viewModel.acceptMicrosoftToken("id-token", "graph-token")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals(setOf(WorkspaceKey.HRIS, WorkspaceKey.INTERNAL_APPS), state.allowedWorkspaces)
        assertEquals(CommonLoginMethod.MICROSOFT_TOKEN, state.loginMethod)
        assertEquals(listOf("id-token" to "graph-token"), hrisRepository.microsoftCalls)
        assertEquals(listOf("id-token" to "graph-token"), internalAppsRepository.microsoftCalls)
        assertTrue(pendingStore.canEnter(WorkspaceKey.HRIS))
        assertTrue(pendingStore.canEnter(WorkspaceKey.INTERNAL_APPS))
        assertNull(pendingStore.pendingMicrosoftAuth())
    }

    @Test
    fun acceptMicrosoftToken_whenInternalAppsFails_doesNotAuthenticateAndClearsPartialSessions() =
        runTestWithMain {
            val hrisRepository = FakeAuthSessionRepository(
                workspace = WorkspaceKey.HRIS,
                microsoftResult = AuthSessionResult(session = AppSession(accessToken = "hris-token"))
            )
            val internalAppsRepository = FakeAuthSessionRepository(
                workspace = WorkspaceKey.INTERNAL_APPS,
                microsoftResult = AuthSessionResult(errorMessage = "Internal Apps failed.")
            )
            val pendingStore = PendingCommonLoginStore()
            val viewModel = createViewModel(hrisRepository, internalAppsRepository, pendingStore)

            viewModel.acceptMicrosoftToken("id-token", "graph-token")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isAuthenticated)
            assertTrue(state.allowedWorkspaces.isEmpty())
            assertFalse(hrisRepository.hasValidSession())
            assertFalse(internalAppsRepository.hasValidSession())
            assertFalse(pendingStore.canEnter(WorkspaceKey.HRIS))
            assertFalse(pendingStore.canEnter(WorkspaceKey.INTERNAL_APPS))
        }

    @Test
    fun signOut_clearsBothRepositoriesAndPendingWorkspaceAccess() = runTestWithMain {
        val hrisRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.HRIS,
            session = AppSession(accessToken = "hris-token")
        )
        val internalAppsRepository = FakeAuthSessionRepository(
            workspace = WorkspaceKey.INTERNAL_APPS,
            session = AppSession(accessToken = "internal-token")
        )
        val pendingStore = PendingCommonLoginStore()
        val viewModel = createViewModel(hrisRepository, internalAppsRepository, pendingStore)

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.session)
        assertTrue(state.allowedWorkspaces.isEmpty())
        assertFalse(hrisRepository.hasValidSession())
        assertFalse(internalAppsRepository.hasValidSession())
        assertFalse(pendingStore.canEnter(WorkspaceKey.HRIS))
        assertFalse(pendingStore.canEnter(WorkspaceKey.INTERNAL_APPS))
    }

    private fun createViewModel(
        hrisRepository: FakeAuthSessionRepository,
        internalAppsRepository: FakeAuthSessionRepository,
        pendingStore: PendingCommonLoginStore = PendingCommonLoginStore()
    ): CommonLoginViewModel {
        return CommonLoginViewModel(
            FakeWorkspaceAuthSessionRepositoryProvider(hrisRepository, internalAppsRepository),
            pendingStore,
            FakeLoginCredentialStore()
        )
    }

    private fun runTestWithMain(block: suspend TestScope.() -> Unit) {
        runTest(dispatcher, testBody = block)
    }
}

private class FakeWorkspaceAuthSessionRepositoryProvider(
    private val hrisRepository: AuthSessionRepository,
    private val internalAppsRepository: AuthSessionRepository
) : WorkspaceAuthSessionRepositoryProvider {
    override fun repositoryFor(workspace: WorkspaceKey): AuthSessionRepository {
        return when (workspace) {
            WorkspaceKey.HRIS -> hrisRepository
            WorkspaceKey.INTERNAL_APPS -> internalAppsRepository
        }
    }
}

private class FakeAuthSessionRepository(
    override val workspace: WorkspaceKey,
    private var session: AppSession? = null,
    private val passwordResult: AuthSessionResult = AuthSessionResult(errorMessage = "Password failed."),
    private val microsoftResult: AuthSessionResult = AuthSessionResult(errorMessage = "Microsoft failed.")
) : AuthSessionRepository {
    val passwordCalls = mutableListOf<Pair<String, String>>()
    val microsoftCalls = mutableListOf<Pair<String, String?>>()

    override fun hasValidSession(): Boolean = session != null

    override fun currentSession(): AppSession? = session

    override suspend fun signInWithPassword(
        username: String,
        password: String
    ): AuthSessionResult {
        passwordCalls += username to password
        session = passwordResult.session
        return passwordResult
    }

    override suspend fun signInWithMicrosoftToken(
        idToken: String,
        graphAccessToken: String?
    ): AuthSessionResult {
        microsoftCalls += idToken to graphAccessToken
        session = microsoftResult.session
        return microsoftResult
    }

    override suspend fun signOut(): AuthSessionResult {
        session = null
        return AuthSessionResult()
    }

    override fun clearSession() {
        session = null
    }
}

private class FakeLoginCredentialStore : LoginCredentialStore {
    override fun getRememberedEmail(): String = ""
    override fun getRememberedPassword(): String = ""
    override fun isRememberMeEnabled(): Boolean = false
    override fun saveRememberedCredentials(email: String, password: String) = Unit
    override fun clearRememberedCredentials() = Unit
}
