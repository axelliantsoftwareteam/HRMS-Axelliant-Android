package com.axelliant.android_erp.di

import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.network.ApiHandler
import com.axelliant.android_erp.network.ApiInterface
import com.axelliant.android_erp.viewmodel.LoginViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Components :KoinComponent{
    val testModelInjection: TestModelInjection by inject()
    val globalConfig : GlobalConfig by inject()
    val apiInterface : ApiInterface by inject()

}