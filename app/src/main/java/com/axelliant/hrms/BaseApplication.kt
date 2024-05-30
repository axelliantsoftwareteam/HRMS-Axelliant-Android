package com.axelliant.hrms

import android.app.Application
import com.axelliant.hrms.di.factoryModule
import com.axelliant.hrms.di.singletonModule
import com.axelliant.hrms.di.viewModelModule
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