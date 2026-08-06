package com.axelliant.hris.di

import android.content.Context
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.core.contracts.auth.AuthContract
import com.axelliant.hris.core.contracts.auth.HrisAuthContractAdapter
import com.axelliant.hris.core.contracts.designsystem.DesignSystemContract
import com.axelliant.hris.core.contracts.designsystem.HrisDesignSystemContractAdapter
import com.axelliant.hris.core.contracts.navigation.AppNavigationContract
import com.axelliant.hris.core.contracts.navigation.HrisAppNavigationContractAdapter
import com.axelliant.hris.core.contracts.session.AppSessionContractAdapter
import com.axelliant.hris.core.contracts.session.SessionContract
import com.axelliant.hris.network.ApiHandler
import com.axelliant.hris.network.ApiInterface
import com.axelliant.hris.utils.SessionManager
import com.axelliant.hris.utils.Validator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HrisCoreModule {

    @Provides
    @Singleton
    fun provideApiInterface(): ApiInterface =
        requireNotNull(ApiHandler.getApiInterface())

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager =
        SessionManager(context)

    @Provides
    @Singleton
    fun provideSessionContract(
        adapter: AppSessionContractAdapter
    ): SessionContract = adapter

    @Provides
    @Singleton
    fun provideAuthContract(
        adapter: HrisAuthContractAdapter
    ): AuthContract = adapter

    @Provides
    @Singleton
    fun provideAppNavigationContract(
        adapter: HrisAppNavigationContractAdapter
    ): AppNavigationContract = adapter

    @Provides
    @Singleton
    fun provideDesignSystemContract(
        adapter: HrisDesignSystemContractAdapter
    ): DesignSystemContract = adapter

    @Provides
    @Singleton
    fun provideGlobalConfig(): GlobalConfig = GlobalConfig.getInstance()

    @Provides
    @Singleton
    fun provideValidator(): Validator = Validator()
}
