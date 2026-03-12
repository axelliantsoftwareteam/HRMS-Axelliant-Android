package com.axelliant.hris.model.attendance

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
    val custom_attendance_status : String,
    val working_hours: Double,
    val display_status: String = "",
    val off_day_type: String = "",
    val worked_on_off_day: Boolean = false,
    val time_zone: String = "",
    val in_time_iso: String = "",
    val out_time_iso: String = "",
    val expected_in_iso: String = "",
    val expected_out_iso: String = "",
    var isDetailVisible:Boolean=false,
    val attendance_reason :String = "",
    val attendance_location :String = "",  // In office , work from home
    val attendance_type :String = ""   // In, Out


)
