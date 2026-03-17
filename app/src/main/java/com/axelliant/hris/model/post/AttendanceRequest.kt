package com.axelliant.hris.model.post

class AttendanceRequest {
        var date_time: String?=null
        var log_type: String?=null
        var location: String?=null
        var attendance_reason: String?=null
        var request_status: String?="Pending"
        var checkin_id: String?=""
}