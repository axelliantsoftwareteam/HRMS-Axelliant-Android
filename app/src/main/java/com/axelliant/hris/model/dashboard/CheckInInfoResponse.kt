package com.axelliant.hris.model.dashboard

data class CheckInInfoResponse(
    val check_in: String,
    val check_out: String,
    val location: String,
    val is_check_in_button: Boolean?=null,
    val is_check_out_button: Boolean?=null

)


