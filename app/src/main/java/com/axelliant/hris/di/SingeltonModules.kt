package com.axelliant.hris.di

import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.network.ApiHandler
import com.axelliant.hris.repos.AttendanceRepo
import com.axelliant.hris.repos.ExpenseRepo
import com.axelliant.hris.repos.HomeRepo
import com.axelliant.hris.repos.LeaveRepo
import com.axelliant.hris.repos.LoginRepo
import com.axelliant.hris.repos.RequestRepo
import com.axelliant.hris.repos.ResourceManageRepo
import com.axelliant.hris.utils.SessionManager
import com.axelliant.hris.utils.Validator
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.axelliant.hris.viewmodel.BaseViewModel
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.axelliant.hris.viewmodel.HomeViewModel
import com.axelliant.hris.viewmodel.LeaveViewModel
import com.axelliant.hris.viewmodel.LoginViewModel
import com.axelliant.hris.viewmodel.RequestViewModel
import com.axelliant.hris.viewmodel.ResourceManageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val singletonModule = module {
    single { TestModelInjection() }
    single { GlobalConfig.getInstance() }
    single { ApiHandler.getApiInterface() }
    single { SessionManager(androidContext()) }
    single { Validator() }


}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { AttendanceViewModel(get()) }
    viewModel { LeaveViewModel(get()) }
    viewModel { ExpenseViewModel(get(),get()) }
    viewModel { RequestViewModel(get()) }
    viewModel { BaseViewModel() }
    viewModel { ResourceManageViewModel(get(),get()) }


}

val factoryModule = module {
    factory { LoginRepo(get()) }
    factory { HomeRepo(get()) }
    factory { AttendanceRepo(get()) }
    factory { LeaveRepo(get()) }
    factory { ExpenseRepo(get()) }
    factory { RequestRepo(get()) }
    factory { ResourceManageRepo(get()) }

}