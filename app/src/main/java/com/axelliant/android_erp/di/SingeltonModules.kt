package com.axelliant.android_erp.di

import org.koin.dsl.module

val singleModule = module {
    single { TestModelInjection() }
}