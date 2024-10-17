package com.axelliant.hris.config

import androidx.navigation.NavController
import com.axelliant.hris.model.dashboard.EmployProfile


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
                true
            else
                currEmployee!!.is_manager
        }

        fun getReportingEmploys(): ArrayList<EmployProfile> {

            val employeeList: ArrayList<EmployProfile> = arrayListOf()
            employeeList.add(EmployProfile().apply { this.employee_name = "All Team" })
            return if (currEmployee == null)
                employeeList
            else if (currEmployee!!.reporting_to_emp == null)
                employeeList
            else {
                employeeList.addAll(currEmployee!!.reporting_to_emp!!)
                employeeList
            }


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