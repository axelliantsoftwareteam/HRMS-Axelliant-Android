package com.axelliant.hris.core.di

import com.axelliant.hris.BuildConfig
import com.axelliant.hris.core.contracts.network.ApiErrorMapperContract
import com.axelliant.hris.core.network.AuthInterceptor
import com.axelliant.hris.core.network.ConnectivityNetworkMonitor
import com.axelliant.hris.core.network.DefaultApiErrorMapper
import com.axelliant.hris.core.network.DevSslConfigurator
import com.axelliant.hris.core.network.NetworkMonitor
import com.axelliant.hris.features.agent.data.remote.AgentChatApiService
import com.axelliant.hris.features.auth.data.remote.AuthApiService
import com.axelliant.hris.features.calendar.data.remote.GraphCalendarApiService
import com.axelliant.hris.features.dashboard.data.remote.UserPermissionApiService
import com.axelliant.hris.features.inventory.assets.data.remote.AssetsApiService
import com.axelliant.hris.features.inventory.products.data.remote.ProductsApiService
import com.axelliant.hris.features.profiles.data.remote.ProfilesApiService
import com.axelliant.hris.features.purchaseorders.data.remote.PurchaseOrderApiService
import com.axelliant.hris.features.quotes.data.remote.CreateQuoteApiService
import com.axelliant.hris.features.quotes.data.remote.QuotesApiService
import com.axelliant.hris.features.quotes.data.remote.WorkflowApiService
import com.axelliant.hris.features.saleorders.data.remote.SaleOrderApiService
import com.axelliant.hris.features.warehouse.data.remote.WarehouseApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 60L

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor
    ): NetworkMonitor = connectivityNetworkMonitor

    @Provides
    @Singleton
    fun provideApiErrorMapper(
        defaultApiErrorMapper: DefaultApiErrorMapper
    ): ApiErrorMapperContract = defaultApiErrorMapper

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .also { DevSslConfigurator.applyIfNeeded(it) }
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("graphOkHttpClient")
    fun provideGraphOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("graphRetrofit")
    fun provideGraphRetrofit(
        @Named("graphOkHttpClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideGraphCalendarApiService(
        @Named("graphRetrofit") retrofit: Retrofit
    ): GraphCalendarApiService = retrofit.create(GraphCalendarApiService::class.java)

    @Provides
    @Singleton
    fun provideUserPermissionApiService(retrofit: Retrofit): UserPermissionApiService =
        retrofit.create(UserPermissionApiService::class.java)

    @Provides
    @Singleton
    fun provideAgentChatApiService(retrofit: Retrofit): AgentChatApiService =
        retrofit.create(AgentChatApiService::class.java)

    @Provides
    @Singleton
    fun provideProfilesApiService(retrofit: Retrofit): ProfilesApiService =
        retrofit.create(ProfilesApiService::class.java)

    @Provides
    @Singleton
    fun provideProductsApiService(retrofit: Retrofit): ProductsApiService =
        retrofit.create(ProductsApiService::class.java)

    @Provides
    @Singleton
    fun provideAssetsApiService(retrofit: Retrofit): AssetsApiService =
        retrofit.create(AssetsApiService::class.java)

    @Provides
    @Singleton
    fun provideQuotesApiService(retrofit: Retrofit): QuotesApiService =
        retrofit.create(QuotesApiService::class.java)

    @Provides
    @Singleton
    fun provideCreateQuoteApiService(retrofit: Retrofit): CreateQuoteApiService =
        retrofit.create(CreateQuoteApiService::class.java)

    @Provides
    @Singleton
    fun provideWorkflowApiService(retrofit: Retrofit): WorkflowApiService =
        retrofit.create(WorkflowApiService::class.java)

    @Provides
    @Singleton
    fun providePurchaseOrderApiService(retrofit: Retrofit): PurchaseOrderApiService =
        retrofit.create(PurchaseOrderApiService::class.java)

    @Provides
    @Singleton
    fun provideSaleOrderApiService(retrofit: Retrofit): SaleOrderApiService =
        retrofit.create(SaleOrderApiService::class.java)

    @Provides
    @Singleton
    fun provideWarehouseApiService(retrofit: Retrofit): WarehouseApiService =
        retrofit.create(WarehouseApiService::class.java)
}
