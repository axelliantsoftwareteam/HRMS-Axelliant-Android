package com.axelliant.android_erp.di

import com.axelliant.android_erp.config.GlobalConfig
import org.koin.dsl.module

val singleModule = module {
    single { TestModelInjection() }
    single { GlobalConfig.getInstance() }
}