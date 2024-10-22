package com.axelliant.hris

import android.app.Application
import com.axelliant.hris.di.factoryModule
import com.axelliant.hris.di.singletonModule
import com.axelliant.hris.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BaseApplication)
            modules(listOf(singletonModule, viewModelModule, factoryModule))
        }
    }
}