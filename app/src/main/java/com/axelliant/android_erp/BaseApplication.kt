package com.axelliant.android_erp

import android.app.Application
import com.axelliant.android_erp.di.singleModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BaseApplication)
            modules(listOf(singleModule))
        }
    }
}