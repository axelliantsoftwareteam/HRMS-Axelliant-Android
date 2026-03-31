package com.axelliant.hris.enums

enum class LeaveStatus (val value: String){
    PENDING("Pending"),
    DRAFT("Draft"),
    Open("Open"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CHECKIN("Check In"),
    CHECKOUT("Check Out"),
    Absent("Absent"),
    Present("Present"),
    OnLeave("OnLeave"),
    Submit("Submit")
}
