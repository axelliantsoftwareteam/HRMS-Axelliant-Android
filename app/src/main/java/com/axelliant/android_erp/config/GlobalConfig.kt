package com.axelliant.android_erp.config

import androidx.navigation.NavController


class GlobalConfig  {

    lateinit var navController: NavController

    companion object{
        private var instance: GlobalConfig? = null
        var isManager :Boolean =true

        fun getInstance(): GlobalConfig {
            if (instance == null) {
                instance = GlobalConfig()
            }
            return instance as GlobalConfig
        }
    }




}