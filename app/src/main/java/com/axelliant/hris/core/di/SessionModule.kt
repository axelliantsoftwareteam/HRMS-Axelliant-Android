package com.axelliant.hris.core.di

import com.axelliant.hris.core.contracts.session.SessionExpiryContract
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.session.DefaultWorkspaceSessionProvider
import com.axelliant.hris.core.session.HrisSessionStore
import com.axelliant.hris.core.session.SessionExpirationHandler
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.core.session.SharedPrefsHrisSessionStore
import com.axelliant.hris.core.session.SharedPrefsSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindSessionManager(
        implementation: SharedPrefsSessionManager
    ): SessionManager

    @Binds
    @Singleton
    abstract fun bindHrisSessionStore(
        implementation: SharedPrefsHrisSessionStore
    ): HrisSessionStore

    @Binds
    @Singleton
    abstract fun bindWorkspaceSessionProvider(
        implementation: DefaultWorkspaceSessionProvider
    ): WorkspaceSessionProvider

    @Binds
    @Singleton
    abstract fun bindSessionExpiryContract(
        implementation: SessionExpirationHandler
    ): SessionExpiryContract
}
