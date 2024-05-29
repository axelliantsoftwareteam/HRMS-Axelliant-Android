package com.axelliant.hrms.di

import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.network.ApiInterface
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Components :KoinComponent{
    val testModelInjection: TestModelInjection by inject()
    val globalConfig : GlobalConfig by inject()
    val apiInterface : ApiInterface by inject()

}