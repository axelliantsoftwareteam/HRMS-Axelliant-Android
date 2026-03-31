package com.axelliant.hris.model.dashboard

import com.google.gson.annotations.SerializedName

 class EmployProfile{
     val company: String?=null
     val date_of_birth: String?=null
/*     @SerializedName(value = "employee_code", alternate = ["custom_employee_code"])
     val employee_code: String?=null*/
     var employee_name: String?=null
     val gender: String?=null
     val image: String?=null
     val is_manager: Boolean=false
     val name: String?=null
     val user_id: String?=null
     val department: String?=null
     val designation: String?=null
     val reports_to:ReportsTo?=null
     val custom_employee_code:String?=null
     val allow_punch_in:Int?=null
     val reporting_to_emp: ArrayList<EmployProfile>?=null

 }

