package com.axelliant.hrms.di

import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.network.ApiHandler
import com.axelliant.hrms.repos.AttendanceRepo
import com.axelliant.hrms.repos.HomeRepo
import com.axelliant.hrms.repos.LeaveRepo
import com.axelliant.hrms.repos.LoginRepo
import com.axelliant.hrms.repos.RequestRepo
import com.axelliant.hrms.utils.SessionManager
import com.axelliant.hrms.viewmodel.AttendanceViewModel
import com.axelliant.hrms.viewmodel.BaseViewModel
import com.axelliant.hrms.viewmodel.HomeViewModel
import com.axelliant.hrms.viewmodel.LeaveViewModel
import com.axelliant.hrms.viewmodel.LoginViewModel
import com.axelliant.hrms.viewmodel.RequestViewModel
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
    viewModel { RequestViewModel(get()) }
    viewModel { BaseViewModel() }

}

val factoryModule = module {
    factory { LoginRepo(get()) }
    factory { HomeRepo(get()) }
    factory { AttendanceRepo(get()) }
    factory { LeaveRepo(get()) }
    factory { RequestRepo(get()) }

}