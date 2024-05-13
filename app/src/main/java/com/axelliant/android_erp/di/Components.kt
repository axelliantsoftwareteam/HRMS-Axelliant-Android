package com.axelliant.android_erp.di

import com.axelliant.android_erp.config.GlobalConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Components :KoinComponent{
    val testModelInjection: TestModelInjection by inject()
    val globalConfig : GlobalConfig by inject()
}