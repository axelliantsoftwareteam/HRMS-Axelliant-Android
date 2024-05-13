package com.axelliant.android_erp.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Components :KoinComponent{
    val testModelInjection: TestModelInjection by inject()
}