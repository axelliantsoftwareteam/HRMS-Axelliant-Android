package com.axelliant.hris.model.login

data class CheckInRequest (
    var log_type: String?=null,
    var date_time: String?=null,
    var location: String?=null,
    var request_status: String?=null,
    var attendance_reason: String?=null

)