package com.axelliant.hris.core.di

import com.axelliant.hris.core.auth.DefaultWorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.core.auth.DefaultHrisTokenProvider
import com.axelliant.hris.core.auth.GlobalLogoutCoordinator
import com.axelliant.hris.core.auth.HrisTokenProvider
import com.axelliant.hris.core.auth.LogoutCoordinator
import com.axelliant.hris.core.contracts.auth.WorkspaceAuthSessionRepositoryProvider
import com.axelliant.hris.features.auth.data.local.LoginCredentialStore
import com.axelliant.hris.features.auth.data.local.LoginPreferences
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthClient
import com.axelliant.hris.features.auth.microsoft.MicrosoftAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthSessionModule {
    @Binds
    @Singleton
    abstract fun bindWorkspaceAuthSessionRepositoryProvider(
        implementation: DefaultWorkspaceAuthSessionRepositoryProvider
    ): WorkspaceAuthSessionRepositoryProvider

    @Binds
    @Singleton
    abstract fun bindHrisTokenProvider(
        implementation: DefaultHrisTokenProvider
    ): HrisTokenProvider

    @Binds
    @Singleton
    abstract fun bindLoginCredentialStore(
        implementation: LoginPreferences
    ): LoginCredentialStore

    @Binds
    @Singleton
    abstract fun bindMicrosoftAuthClient(
        implementation: MicrosoftAuthManager
    ): MicrosoftAuthClient

    @Binds
    @Singleton
    abstract fun bindLogoutCoordinator(
        implementation: GlobalLogoutCoordinator
    ): LogoutCoordinator
}
