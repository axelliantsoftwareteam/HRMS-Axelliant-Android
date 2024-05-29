package com.axelliant.android_erp.di

import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.network.ApiHandler
import com.axelliant.android_erp.repos.AttendanceRepo
import com.axelliant.android_erp.repos.HomeRepo
import com.axelliant.android_erp.repos.LeaveRepo
import com.axelliant.android_erp.repos.LoginRepo
import com.axelliant.android_erp.utils.SessionManager
import com.axelliant.android_erp.viewmodel.AttendanceViewModel
import com.axelliant.android_erp.viewmodel.BaseViewModel
import com.axelliant.android_erp.viewmodel.HomeViewModel
import com.axelliant.android_erp.viewmodel.LeaveViewModel
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singletonModule = module {
    single { TestModelInjection() }
    single { GlobalConfig.getInstance() }
    single { ApiHandler.getApiInterface() }
    single { SessionManager(androidContext()) }

}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { AttendanceViewModel(get()) }
    viewModel { LeaveViewModel(get()) }
    viewModel { BaseViewModel() }

}

val factoryModule = module {
    factory { LoginRepo(get()) }
    factory { HomeRepo(get()) }
    factory { AttendanceRepo(get()) }
    factory { LeaveRepo(get()) }

}