package com.axelliant.hris.di

import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.network.ApiInterface
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Components :KoinComponent{
    val testModelInjection: TestModelInjection by inject()
    val globalConfig : GlobalConfig by inject()
    val apiInterface : ApiInterface by inject()

}