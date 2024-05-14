package com.axelliant.android_erp.di

import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.network.ApiHandler
import com.axelliant.android_erp.repos.LoginRepo
import com.axelliant.android_erp.viewmodel.BaseViewModel
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singletonModule = module {
    single { TestModelInjection() }
    single { GlobalConfig.getInstance() }
    single { ApiHandler.getApiInterface() }


}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { BaseViewModel() }

}

val factoryModule = module {
    factory { LoginRepo(get()) }

}