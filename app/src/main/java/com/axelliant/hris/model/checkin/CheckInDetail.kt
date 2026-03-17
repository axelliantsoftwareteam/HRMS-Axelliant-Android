package com.axelliant.hris.model.checkin

data class CheckInDetail(
    val creation: String,
    val name: String,
    val requeststatus: String,
    val employee: String,
    val designation: String,

    val employee_name: String,
    val time: String,
    val log_type: String,
    val location: String,
    val reason: String,
    val img: String,
    val working_hours: Double,
    var isDetailVisible:Boolean =false


)

