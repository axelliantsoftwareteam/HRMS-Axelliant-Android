package com.axelliant.hrms.di

import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.network.ApiHandler
import com.axelliant.hrms.repos.AttendanceRepo
import com.axelliant.hrms.repos.HomeRepo
import com.axelliant.hrms.repos.LeaveRepo
import com.axelliant.hrms.repos.LoginRepo
import com.axelliant.hrms.viewmodel.AttendanceViewModel
import com.axelliant.hrms.viewmodel.BaseViewModel
import com.axelliant.hrms.viewmodel.HomeViewModel
import com.axelliant.hrms.viewmodel.LeaveViewModel
import com.axelliant.hrms.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singletonModule = module {
    single { TestModelInjection() }
    single { GlobalConfig.getInstance() }
    single { ApiHandler.getApiInterface() }


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