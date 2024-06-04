package com.axelliant.hrms.model.attendance

data class AttendanceDetail(
    val code: String,
    val date: String,
    val employee_name: String,
    val expected_in: String,
    val expected_out: String,
    val in_time: String,
    val out_time: String,
    val requested: String,
    val shift: String,
    val shift_timings: String,
    val status: String,
    val working_hours: Double,
    var isDetailVisible:Boolean=false
)