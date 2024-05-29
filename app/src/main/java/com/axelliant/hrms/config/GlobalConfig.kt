package com.axelliant.hrms.config

import androidx.navigation.NavController
import com.axelliant.hrms.model.dashboard.EmployProfile


class GlobalConfig {

    lateinit var navController: NavController

    companion object {
        private var instance: GlobalConfig? = null
        private var currEmployee: EmployProfile? = null

        fun setCurrentEmployee(employee: EmployProfile) {
            currEmployee = employee
        }

        fun isCurrentManager(): Boolean {
            return if (currEmployee == null)
                false
            else
                currEmployee!!.is_manager
        }

        fun currentEmployeeId(): String {
            return if (currEmployee == null)
                ""
            else
                currEmployee!!.name.toString()
        }

        fun getInstance(): GlobalConfig {
            if (instance == null) {
                instance = GlobalConfig()
            }
            return instance as GlobalConfig
        }
    }


}