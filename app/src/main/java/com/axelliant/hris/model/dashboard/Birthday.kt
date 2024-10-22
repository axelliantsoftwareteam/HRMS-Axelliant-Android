package com.axelliant.hris.model.dashboard

data class Birthday(

    val date_of_birth: String,
    val employee_name: String,
    val image: String?=null,
    val name: String,
    val unix_date_of_birth: Long

)


